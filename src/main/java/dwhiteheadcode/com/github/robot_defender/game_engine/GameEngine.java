package dwhiteheadcode.com.github.robot_defender.game_engine;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dwhiteheadcode.com.github.robot_defender.entities.robot.*;
import dwhiteheadcode.com.github.robot_defender.*;
import dwhiteheadcode.com.github.robot_defender.entities.fortress_wall.*;
import dwhiteheadcode.com.github.robot_defender.misc.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;


public class GameEngine implements ArenaListener
{
    // CONSTANTS
    public static final int MAX_WALLS_DEFAULT = 10;
    public static final int NUM_ROWS_DEFAULT = 9;
    public static final int NUM_COLS_DEFAULT = 9;
    public static final String GAME_OVER_SOUND_FILE = "sounds/game_over.wav";

    // UI
    private GameWindow gameWindow;
    private JFXArena arena;

    // SOUNDS
    private MediaPlayer gameOverMediaPlayer;

    // GAME ENGINE THREADS
    private volatile Thread robotSpawnProducerThread;
    private volatile Thread robotSpawnConsumerThread;
    private volatile Thread wallSpawnConsumerThread;  
    private volatile Thread wallSpawnProducerThread;
    private volatile Thread scoreThread;

    // WALL SPAWNER
    private FortressWallSpawner wallSpawner;

    // ROBOT THREAD POOL
    private ExecutorService robotExecutorService;

    // BLOCKING QUEUE
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(5); // robot-spawn-producer -> robot-spawn-consumer
    private BlockingQueue<FortressWall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(10); // wall-spawn-producer -> wall-spawn-consumer

    // GAME STATE INFO - Considered to be one resource. Locked with gameStateMutex; unless otherwise specified
    private Location[][] gridSquares;
    private Map<Integer, Future<?>> robotFutures = new HashMap<>(); // A map of all robot TASKS (futures). Robot ID is used as key. Locked with robotFuturesMutex (and not gameStateMutex).
    private Map<Integer, Robot> robots = new HashMap<>(); // A map of all active robots. Robot ID is used as key
    private List<FortressWall> placedWalls = Collections.synchronizedList(new ArrayList<>()); // A list of all active walls

    private ScoreCalculator score; // Handles its own locking

    private final Vector2d citadel; // Can't be modified, so doesn't need to be locked

    private final int numRows; // Can't be modified, so doesn't need to be locked
    private final int numCols; // Can't be modified, so doesn't need to be locked

    // MUTEXES
    private Object gameStateMutex = new Object(); // Used to lock GAME STATE INFO variables, unless otherwise specified
    private Object robotFuturesMutex = new Object(); // Used to lock robotFutures
   


    //CONSTRUCTORS
    public static GameEngine instance(GameWindow app)
    {
        return new GameEngine(app, NUM_ROWS_DEFAULT, NUM_COLS_DEFAULT);
    }

    public static GameEngine instance(GameWindow app, int numRows, int numCols)
    {
        return new GameEngine(app, numRows, numCols);
    }

    private GameEngine(GameWindow app, int numRows, int numCols)
    {
        if(numRows < 3)
        {
            throw new IllegalArgumentException("GameEngine only supports grids with at least 3 rows.");
        }

        if(numCols < 3)
        {
            throw new IllegalArgumentException("GameEngine only supports grids with at least 3 columns.");
        }

        this.gameWindow = app;

        this.numRows = numRows;
        this.numCols = numCols;

        int numSquares = numRows * numCols;

        // Create a thread pool for robot threads
        // Min 4 threads, max threads = numSquares - 1 (this is the maximum number of robots).
        // Destroy unused threads after 10 seconds
        this.robotExecutorService = new ThreadPoolExecutor(
            4, (numSquares - 1),
            10, TimeUnit.SECONDS,
            new SynchronousQueue<>()
        );

        initGridSquares(numRows, numCols);

        //Set the citadel in the middle square. If even rows, favour row under middle; if even cols, favour col right of middle.
        int middleRow = (numRows / 2);
        int middleCol = (numCols / 2);
        this.gridSquares[middleRow][middleCol].setCitadel(true);
        this.citadel = new Vector2d(middleCol, middleRow);

        loadGameOverSound();
    }

    private void loadGameOverSound()
    {
        URL gameOverSoundUri = getClass().getClassLoader().getResource(GAME_OVER_SOUND_FILE);
        Media gameOverSound = new Media(gameOverSoundUri.toString());
        this.gameOverMediaPlayer = new MediaPlayer(gameOverSound);
        this.gameOverMediaPlayer.setVolume(0.1);
    }

    public int getNumRows()
    {
        return this.numRows;
    }

    public int getNumCols()
    {
        return this.numCols;
    }


    public void setArena(JFXArena arena)
    {
        if(this.arena != null)
        {
            throw new IllegalStateException("GameEngine's Arena has already been set.");
        }

        this.arena = arena;
    }

    /*
     * Initialises the array and elements that make up the grid.
     * 
     * Thread: Runs in the thread that called GameEngine constructor (i.e. UI thread from App). 
     *         Doesn't need synchronize, as start() can't have been called prior, 
     *         so only this thread is accessing resources
     */
    private void initGridSquares(int numRows, int numCols)
    {
        if(this.gridSquares != null)
        {
            throw new IllegalStateException("Grid squares already initialised.");
        }

        //Initialise array
        this.gridSquares = new Location[numRows][numCols];

        for(int i = 0; i < numCols; i++)
        {
            for(int j = 0; j < numRows; j++)
            {
                this.gridSquares[i][j] = new Location( new Vector2d(i, j) );
            }
        }
    }

    /*
     * Starts the threads necessary for the game engine to function.
     * 
     * This includes:
     *     - robotSpawnConsumerThread
     *     - robotSpawnProducerThread
     *     - wallSpawnConsumerThread
     *     - wallSpawnProducerThread
     *     - scoreThread
     * 
     * Note: robotSpawnConsumerThread allocates additional tasks to robotExecutorService threads
     */
    public void start()
    {
        if(robotSpawnProducerThread != null || robotSpawnConsumerThread != null || wallSpawnConsumerThread != null || wallSpawnProducerThread != null || scoreThread != null)
        {
            throw new IllegalStateException("Can't start a GameEngine that is already running.");
        }

        // Create robot producer and consumer
        robotSpawnConsumerThread = new Thread(robotSpawnConsumerRunnable(), "robot-spawn-consumer");
        robotSpawnProducerThread = new Thread( new RobotSpawner(this), "robot-spawn-producer" );
        
        // Create wall producer and consumer
        this.wallSpawner = new FortressWallSpawner(this, MAX_WALLS_DEFAULT);
        wallSpawnProducerThread = new Thread(wallSpawner, "wall-spawn-producer");
        wallSpawnConsumerThread = new Thread(wallSpawnConsumerRunnable(), "wall-spawn-consumer");

        // Create score thread
        this.score = new ScoreCalculator(this);
        scoreThread = new Thread(this.score, "score-thread");

        // Start all threads
        robotSpawnConsumerThread.start();
        robotSpawnProducerThread.start();
        wallSpawnConsumerThread.start();
        wallSpawnProducerThread.start();
        scoreThread.start();
    }
    
    /*
     * Interrupts all actively running Threads. Shuts down the ExecutorService used for Robots.
     * 
     * Note: 
     * This does not impact the game state in any way (such as removing robots from the grid, or removing robot tasks from the robotFutures Map), 
     * as it is assumed that this GameEngine won't be used again. This has the added benefit of not removing robots from the screen after a gameover(),
     * allowing the player to see where robots were at the time of the gameover. 
     */
    public void stop()
    {
        if(robotSpawnConsumerThread == null || robotSpawnProducerThread == null || wallSpawnConsumerThread == null || wallSpawnProducerThread == null || scoreThread == null)
        {
            throw new IllegalStateException("Can't stop a GameEngine that hasn't started.");
        }

        // Interrupts all Robot threads
        robotExecutorService.shutdownNow();

        // Interrupts all GameEngine threads
        robotSpawnConsumerThread.interrupt();
        robotSpawnProducerThread.interrupt();
        wallSpawnConsumerThread.interrupt();
        wallSpawnProducerThread.interrupt();    
        scoreThread.interrupt();
    }
 
    
    /*
     * Returns a runnable containing the logic for the robot spawn consumer.
     * 
     * This Runnable represents an "infinite" (interruptible) loop that takes a new robot from 
     * robotSpawnBlockingQueue whenever it is available, then places it in a random, available
     * corner in the map.
     * If no corner is available (not occupied by another robot), it will wait() on the lock,
     * expecting a notification from any other thread that has updated 'gridSquares'.
     * 
     * When placing a robot, this Runnable does the following:
     *     - Sets the Robot's coordinates
     *     - Updates the relevant 'gridSquares' Location (with Location.setRobot())
     *     - Saves a reference to the Robot in 'robots'
     *     - Starts the robot's Runnable using the thread pool, and stores its
     *       future in 'robotFutures'
     *     - Displays a message in the on screen text log.
     *     - Checks if there is a wall on the spawn point. If so:
     *            - Damages the wall (which destroys it if already damaged)
     *            - Destroys the Robot
     *     - Updates the UI
     */
    private Runnable robotSpawnConsumerRunnable()
    {
        return () -> 
        {
            Random rand = new Random();

            try
            {
                while(true)
                {
                    Robot nextRobot = robotSpawnBlockingQueue.take();
                    
                    boolean startRobot = true; // Set to false if the robot spawns on a wall and is immediately destroyed

                    synchronized(gameStateMutex)
                    {
                        List<Location> unoccupiedCorners;

                        //Get the Locations of the 4 corners of the map
                        Location[] corners = new Location[4];
                        
                        corners[0] = gridSquares[0][0];  // Top left
                        corners[1] = gridSquares[numRows - 1][0]; // Bottom left
                        corners[2] = gridSquares[numRows - 1][numCols - 1]; //Bottom right
                        corners[3] = gridSquares[0][numCols - 1];  // Top right

                        // Wait until there is at least 1 free corner
                        do
                        {
                            // Add corner Locations that are unoccupied by other robots to a list
                            unoccupiedCorners = new ArrayList<>();
                            for(Location l : corners)
                            {                            
                                if(l.getRobot() == null)
                                {
                                    unoccupiedCorners.add(l);
                                }
                            }

                            // If no corner is free, release the lock until a robot moves
                            if(unoccupiedCorners.size() == 0)
                            {
                                gameStateMutex.wait();
                            }
                        }
                        while(unoccupiedCorners.size() == 0);
                       
                        // Randomly choose one of the unoccupied corners as the robot's spawn point
                        int spawnLocationIdx;
                        if(unoccupiedCorners.size() == 1) // If there is only 1 free corner, use it. Separated due to nextInt() max having to be > min
                        {
                            spawnLocationIdx = 0;
                        }
                        else
                        {
                            spawnLocationIdx = rand.nextInt( 0, unoccupiedCorners.size() ); 
                        }

                        // Tell the corner that it is occupied, and tell the robot its coordinates
                        Location spawnLocation = unoccupiedCorners.get(spawnLocationIdx);                        
                        spawnLocation.setRobot(nextRobot);
                        nextRobot.setCoordinates( spawnLocation.getCoordinates() );                        

                        // Add the robot to the map of all robots
                        robots.put(nextRobot.getId(), nextRobot);

                        //Save the coordinates to print to the screen 
                        Vector2d spawnCoords = nextRobot.getCoordinates();

                        // Log robot spawn on screen
                        gameWindow.log("Spawned robot '" + nextRobot.getId() + "' at " + spawnCoords.toString() + "\n");
                                                  
                        // If there is a wall on the spawn point, damage it. 
                        FortressWall wallOnSpawnPoint = spawnLocation.getWall();
                        if(wallOnSpawnPoint != null)
                        {
                            startRobot = false;
                            robotHitWall(nextRobot, wallOnSpawnPoint);
                        }

                                
                    }   
                    
                    //Start the robot, and store a reference to its execution in the map (so it can be interrupted later)
                    synchronized(robotFuturesMutex)
                    {         
                        if(startRobot)
                        {
                            Future<?> f = robotExecutorService.submit(nextRobot);
                            robotFutures.put( nextRobot.getId(), f );
                        }                   
                    }

                    updateArenaUi(); 
                }  
            }
            catch(InterruptedException iE)
            {
                // Nothing needs to be done here. 
                // If the thread is interrupted, we don't need to do anything with 
                // the rest of the queued robots
            }

        };
    }



    /**
     * Returns a Runnable containing the logic for the wall spawn consumer.
     * 
     * This Runnable represents an "infinite" (interruptible) loop that takes a new wall from 
     * wallSpawnBlockingQueue whenever it is available, then places it in the location specified 
     * by the wall's coordinates.
     * 
     * When placing a wall, this Runnable does the following:
     *     - Updates the on screen "Queued Walls" text
     *     - Checks if the Location already had a wall. If so, this is destroyed.
     *     - Places the new wall in its Location (with Location.setWall()) 
     *     - Stores a reference to the wall in 'placedWalls'
     *     - Displays a message in the on screen text log.
     *     - Updates the UI
     */
    private Runnable wallSpawnConsumerRunnable()
    {
        return () -> {
            try
            {
                while(true)
                {
                    FortressWall newWall = wallSpawnBlockingQueue.take();

                    updateQueuedWallsText();
                    updateAvailableWallsText();
                    
                    Vector2d wallPos = newWall.getCoordinates();

                    int wallX = (int)wallPos.x(); // Note: Disregards fractional position. Shouldn't matter if called appropriately
                    int wallY = (int)wallPos.y(); // Same as above

                    synchronized(gameStateMutex)
                    {                        
                        Location location = gridSquares[wallX][wallY];

                        // Ignores the build command if there is a robot at this location
                        if(location.getRobot() == null)
                        {
                            // If this wall replaces an existing wall, remove the old wall
                            FortressWall previousWall = location.getWall();
                            if(previousWall != null)
                            {
                                destroyWall(previousWall);
                            }                   
                            
                            location.setWall(newWall); // Note: If a wall already exists, this assumes a new wall can be placed to "refresh" it (e.g. if it was damamged)
                            placedWalls.add(newWall); 
                            newWall.getPlacementSound().play();

                            gameWindow.log("Spawned wall at (" + wallX + ", " + wallY + ")\n");
                        }                   
                    }

                    updateQueuedWallsText();
                    updateAvailableWallsText();
                    updateArenaUi();
                }
            }
            catch(InterruptedException iE)
            {
                // Nothing needs to be done here. 
                // If the thread is interrupted, we don't need to do anything with 
                // the rest of the queued walls
            }
        };
    }    



    /*
     * Adds a new robot to the blocking queue, for consumption by the robot-spawn-consumer 
     * thread.
     * 
     * Thread: Runs in the calling thread (usually the Robot-Spawn-Producer) 
     */
    public void putNewRobot(Robot robot) throws InterruptedException
    {
        this.robotSpawnBlockingQueue.put(robot);
    }
   

    /*
     * Allows a robot to request to make a move
     * 
     * If the move is valid, this returns true, otherwise returns false. A move is valid if:
     *     - It does not take the robot out of bounds
     *     - The destination Location is not already occupied by a robot
     * 
     * If the move is valid, Robot.setMoveCallback() is called, giving the robot
     * a method to call when it finishes its move (so the GameEngine can perform additional
     * processing, such as collision detection).
     * 
     * Thread: Runs in the calling thread (typically the Robot's thread)
     */
    public boolean requestMove(Robot robot, Vector2d move) throws InterruptedException
    {
        Vector2d startPos = robot.getCoordinates();
        Vector2d endPos = startPos.plus( move );

        int startX = (int)startPos.x(); // Note: Disregards robot's fractional position. Shouldn't matter if called appropriately
        int startY = (int)startPos.y(); // Same as above

        int endX = (int)endPos.x(); // Same as above
        int endY = (int)endPos.y(); // Same as above

        // Discard moves that would put the robot out of bounds
        if(endX < 0 || endX >= numCols || endY < 0 || endY >= numRows)
        {
            return false;
        }

        synchronized(gameStateMutex)
        {            
            Location endLocation = gridSquares[endX][endY];

            // Location already occupied
            if(endLocation.getRobot() != null) 
            {
                return false;
            }

            // Move is valid below this point -------
            // Occupy the end location
            endLocation.setRobot(robot);

            // Give the robot a callback to call upon move completion
            robot.setMoveCallback( ()-> 
            {
                this.moveCompleted(robot, startX, startY, endX, endY);
            });           

            return true;
        }
    }

    /*
     * Called when a robot wants to update its position (called each animation interval)
     * 
     * Updates the position of the robot, and updates the UI
     * 
     * Thread: Runs in the calling thread (Robot)
     */
    public void updateRobotPos(Robot robot, Vector2d newPos)
    {
        synchronized(gameStateMutex)
        {
            robot.setCoordinates(newPos);
        }

        updateArenaUi();    
    }

    /*
     * Runs when a robot finishes its move. 
     * 
     * - Sets the Robot at Location (startX, startY) to null.
     * - Checks for Wall collisions at (endX, endY)
     * - Checks for Citadel collision at (endX, endY)
     * 
     * Thread: Runs in Robot's thread
     */
    public void moveCompleted(Robot robot, int startX, int startY, int endX, int endY)
    {
        synchronized(gameStateMutex)
        {
            Location startLocation = gridSquares[startX][startY];
            Location endLocation = gridSquares[endX][endY];

            // Free the start location
            startLocation.setRobot(null);

            //Check for wall collision
            FortressWall wall = endLocation.getWall();
            if( wall != null)
            {
                robotHitWall(robot, wall);
            }

            // Check for game over
            if(endLocation.hasCitadel())
            {
                gameOver();
            }

            gameStateMutex.notifyAll(); // Notify robot-spawn-consumer that a corner might be free
        }
    }

    /*
     * Tells 'app' to trigger the gameOver sequence.
     * 
     * Thread: "Runs" in the calling thread (typically a Robot's thread)
     */
    private void gameOver()
    {
        int finalScore = score.getScore();
        gameOverMediaPlayer.play();
        gameWindow.gameOver(finalScore);
    }


    /*
     * Destroys a robobt by doing the following:
     *     - Stops its task
     *     - Removes its future from the robotFutures Map
     *     - Calls setRobot(null) on the Location where the Robot was
     *     - Removes the robot from the robots Map
     * 
     * Thread: Called from either:
     *             - The Robot's thread (from GameEngine.moveComplete(), if it moved into a wall)
     *             - Robot-spawn-consumer (if the robot spawned on a wall)
     *             - Whichever thread called GameEnigine.stop() (typically UI)
     */
    private void destroyRobot(Robot robot)
    {
        int id = robot.getId();

        synchronized(robotFuturesMutex)
        {
            // Interrupt the robot's task, and remove it from the map of Futures
            Future<?> future = robotFutures.get(id);
            if(future != null) // May be null if robot spawned on a wall
            {
                future.cancel(true);
                robotFutures.remove(id);
            }            
        }

        synchronized(gameStateMutex)
        {
            // Remove the robot from its location
            int x = (int)robot.getCoordinates().x(); // Ignores fractional part of coordinate. Shouldn't matter if called appropriately
            int y = (int)robot.getCoordinates().y(); // Same as above

            Location location = gridSquares[x][y];
            location.setRobot(null);

            // Remove the robot from the map of robots
            robots.remove(id);
        }

        updateArenaUi();
    }

    /*
     * Called when a robot hits a wall
     * 
     * Increases the score, displays an on-screen log message; then destroys the robot.
     * 
     * Thread: Called by the robot's thread from GameEngine.moveComplete(), or by the robot-spawn-consumer thread
     * if the robot spawned on a wall
     */
    private void robotHitWall(Robot robot, FortressWall wall)
    {
        synchronized(gameStateMutex)
        {
            int id = robot.getId();

            // Remove the robot from its location
            int x = (int)robot.getCoordinates().x(); // Ignores fractional part of coordinate. Shouldn't matter if called appropriately
            int y = (int)robot.getCoordinates().y(); // Same as above

            // Increase the score
            score.robotDestroyed();

            wall.damage();
            destroyRobot(robot);

            // Show log message on screen
            String msg = "Robot '" + id + "' hit a wall at (" + x + ", " + y + ")\n";
            gameWindow.log(msg);
        }

        
    }

    /*
     * Called when a wall needs to be destroyed
     * 
     * - Removes the wall from its Location
     * - Removes the wall from the List of placed walls
     * - Updates the UI
     * 
     * Thread: Called by either the wall-spawn-consumer thread (if a wall is being placed on
     * an existing wall); or by a Robot thread (if a robot has moved onto a pre-damaged wall)
     */
    public void destroyWall(FortressWall wall)
    {
        synchronized(gameStateMutex)
        {
            // Remove the wall from the location
            int x = (int)wall.getCoordinates().x(); // Ignores fractional part of coordinate. Shouldn't matter if called appropriately
            int y = (int)wall.getCoordinates().y(); // Same as above

            Location location = gridSquares[x][y];
            location.setWall(null);

            // Remove the wall from the list of walls 
            placedWalls.remove(wall);
        }

        updateArenaUi();
        updateAvailableWallsText();
    }


    /*
     * Returns a List of all robots in the game (as ReadOnlyRobots)
     * 
     * Thread: Runs in the calling thread (typically UI)
     */
    public List<ReadOnlyRobot> getRobots()
    {
        List<ReadOnlyRobot> list = new ArrayList<>();

        synchronized(gameStateMutex) // Can block....
        {
            for(Robot r : this.robots.values())
            {
                list.add( new ReadOnlyRobot(r) );
            }
        }

        return list;
    }

    /*
     * Returns a List of all FortressWalls in the game (as ReadOnlyFortressWalls)
     * 
     * Thread: Runs in the calling thread (typically UI)
     */
    public List<ReadOnlyFortressWall> getPlacedWalls()
    {
        List<ReadOnlyFortressWall> list = new ArrayList<>();

        synchronized(gameStateMutex) // Can block....
        {
            for(FortressWall w : placedWalls)
            {
                list.add( new ReadOnlyFortressWall(w));
            }
        }

        return list;
    }

    /*
     * Returns the Vector position of the Citadel
     * 
     * Note: As the Citadel location is never updated after GameEngine's constructor
     * is called, this does not need to be locked.
     * 
     * Thread: Robot, UI
     */
    public Vector2d getCitadel()
    {
        return new Vector2d(citadel);
    }


    /*
     * Called when the user wants to place a wall
     * 
     * Thread: Runs in the UI thread
     */
    @Override
    public void squareClicked(int x, int y)
    {
        wallSpawner.requestWall(x, y, getCitadel());
        updateQueuedWallsText();
    }

    /*
     * Updates the on-screen text displaying the number of "queued" walls
     * 
     * Thread: Wall-spawn-consumer, UI
     */
    private void updateQueuedWallsText()
    {
        int numWalls = wallSpawner.queueSize();

        gameWindow.setQueuedWalls(numWalls);
    }

    /*
     * Returns the number of "placed" walls. This actaully returns the number of the walls 
     * that have been placed, plus the number of walls pending placement by the consumer thread
     * (i.e.: those that are in wallSpawnBlockingQueue)
     * 
     * Thread: FortressWallSpawner
     */
    public int getNumSpawnedWalls()
    {
        synchronized(gameStateMutex)
        {
            return wallSpawnBlockingQueue.size() + placedWalls.size();
        }
    }

    /*
     * Returns the number of walls that have either been placed, or are queued to be placed.
     * 
     * Thread: WallSpawnConsumer, Robot, UI
     */
    public int getAllWallsCount()
    {
        synchronized(gameStateMutex)
        {
            return wallSpawnBlockingQueue.size() + placedWalls.size() + wallSpawner.queueSize();
        }
    }

    /*
     * Calculates the number of available walls, and update the UI.
     * 
     * NOTE: If wall A is queued such that it will replace a damaged wall (wall B), both A and B will be
     *       counted as walls (thus 2 will be subtracted from the number of available walls) until wall A 
     *       is placed, which actually destroys wall B.
     */
    public void updateAvailableWallsText()
    {
        int availableWalls = wallSpawner.maxWalls() - getAllWallsCount();
        
        gameWindow.setAvailableWallsText(availableWalls);
    }

    /*
     * Add a new wall to the wallSpawnBlockingQueue, for consumption by wall-spawn-consumer
     * 
     * Thread: Wall-spawn-producer
     */
    public void putNewWall(FortressWall wall) throws InterruptedException
    {
        wallSpawnBlockingQueue.put(wall);
    }

    /*
     * Called to update the UI of the grid portion of the game (i.e.: the arena)
     * 
     * Thread: Wall-spawn-consumer, robot-spawn-consumer, Robots
     */
    public void updateArenaUi()
    {
        gameWindow.updateArenaUi();
    }

    /*
     * Updates the score on-screen
     * 
     * Threads: score
     */
    public void updateScore(int score)
    {
        gameWindow.setScore(score);
    }

    public int getMaxWalls()
    {
        return MAX_WALLS_DEFAULT;
    }

    public void updateWallCooldown(long remainingCooldownMillis)
    {
        if(remainingCooldownMillis < 0)
        {
            throw new IllegalArgumentException("Wall spawn cooldown can't be less than 0.");
        }

        gameWindow.setWallCooldownText(remainingCooldownMillis);   
    }


}
