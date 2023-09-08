package edu.curtin.saed.assignment1.game_engine;

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

import edu.curtin.saed.assignment1.entities.robot.*;
import edu.curtin.saed.assignment1.App;
import edu.curtin.saed.assignment1.ArenaListener;
import edu.curtin.saed.assignment1.JFXArena;
import edu.curtin.saed.assignment1.entities.fortress_wall.*;
import edu.curtin.saed.assignment1.misc.Vector2d;
import edu.curtin.saed.assignment1.misc.Location;
import javafx.application.Platform;

public class GameEngine implements ArenaListener
{
    // UI
    private App app;
    private JFXArena arena;

    // GAME_ENGINE THREADS
    private Thread robotSpawnProducerThread;
    private Thread robotSpawnConsumerThread;
    private Thread wallSpawnConsumerThread;  
    private Thread wallSpawnProducerThread;
    private Thread scoreThread;

    // WALL SPAWNER
    private FortressWallSpawner wallSpawner;

    // THREAD POOL
    private ExecutorService robotExecutorService;

    // BLOCKING QUEUE
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(5);
    private BlockingQueue<FortressWall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(10);

    // GAME STATE INFO - All except "citadel" is considered the same resource, and is locked with gameStateMutex
    private Location[][] gridSquares; // Accessed by robotSpawnConsumerThread, wallSpawnConsumerThread and robot threads.
    private Map<Integer, Future<?>> robotFutures = Collections.synchronizedMap( new HashMap<>() ); // TODO accessed by which threads? Needs synchronizedMap?
    private Map<Integer, Robot> robots = Collections.synchronizedMap(new HashMap<>());
    private List<FortressWall> placedWalls = Collections.synchronizedList(new ArrayList<>()); 
    private ScoreCalculator score;

    private Vector2d citadel; // Is never modified after construction, so does not need to be locked

    private final int numRows;
    private final int numCols;

    // MUTEXES
    private Object gameStateMutex = new Object(); // Used to lock "gridSquares", "robots", "walls" TODO and robotThreads?
   

    //CONSTRUCTOR
    public GameEngine(App app, int numRows, int numCols)
    {
        if(numRows < 3)
        {
            throw new IllegalStateException("GameEngine only supports grids with at least 3 rows.");
        }

        if(numCols < 3)
        {
            throw new IllegalStateException("GameEngine only supports grids with at least 3 cols.");
        }

        this.app = app;

        this.numRows = numRows;
        this.numCols = numCols;

        int numSquares = numRows * numCols;

        // Create a thread pool for robot threads
        // Min 4 threads, max threads = numSquares - 1 (this is the maximum number of robots). TODO Max may need to change?
        // Destroy unused threads after 10 seconds
        this.robotExecutorService = new ThreadPoolExecutor(
            4, (numSquares - 1),
            10, TimeUnit.SECONDS,
            new SynchronousQueue<>()
        );

        initGridSquares(numRows, numCols);
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
     * Sets the citadel in the middle of the grid. 
     * Thread: Runs in the thread that called Constructor (UI thread from App). 
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

        for(int i = 0; i < numRows; i++)
        {
            for(int j = 0; j < numCols; j++)
            {
                this.gridSquares[i][j] = new Location( new Vector2d(j, i) );
            }
        }

        //Set the citadel in the middle square. If even rows, favour row under middle; if even cols, favour col right of middle.
        int middleRow = (numRows / 2);
        int middleCol = (numCols / 2);

        this.gridSquares[middleRow][middleCol].setCitadel(true);

        this.citadel = new Vector2d(middleCol, middleRow);
    }


    public void start()
    {
        if(robotSpawnConsumerThread != null || wallSpawnConsumerThread != null || robotSpawnProducerThread != null)
        {
            throw new IllegalStateException("Can't start a GameEngine that is already running.");
        }

        // Create robot producer and consumer
        robotSpawnConsumerThread = new Thread(robotSpawnConsumerRunnable(), "robot-spawn-consumer");
        robotSpawnProducerThread = new Thread( new RobotSpawner(this), "robot-spawn-producer" );
        
        // Create wall producer and consumer
        wallSpawnConsumerThread = new Thread(wallSpawnConsumerRunnable(), "wall-spawn-consumer");
        this.wallSpawner = new FortressWallSpawner(this);
        wallSpawnProducerThread = new Thread(wallSpawner, "wall-spawn-producer");

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

    public void updateScore(int score)
    {
        Platform.runLater( () -> {app.setScore(score);});
    }

    public void stop()
    {
        if(robotSpawnConsumerThread == null || robotSpawnProducerThread == null || wallSpawnConsumerThread == null || wallSpawnProducerThread == null || scoreThread == null)
        {
            throw new IllegalStateException("Can't stop a GameEngine that hasn't started.");
        }

        // Interrupt all the robot threads
        for(Future<?> robotFuture : robotFutures.values())
        {
            robotFuture.cancel(true);
        }

        robotSpawnConsumerThread.interrupt();
        robotSpawnProducerThread.interrupt();
        wallSpawnConsumerThread.interrupt();
        wallSpawnProducerThread.interrupt();    
        scoreThread.interrupt();
    }

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

                    synchronized(gameStateMutex)
                    {
                        List<Location> unoccupiedCorners;

                        // Wait until there is at least 1 free corner
                        do
                        {
                            //Get the Locations of the 4 corners of the map
                            Location[] corners = new Location[4];
                            
                            corners[0] = gridSquares[0][0];  // Top left
                            corners[1] = gridSquares[numRows - 1][0]; // Bottom left
                            corners[2] = gridSquares[numRows - 1][numCols - 1]; //Bottom right
                            corners[3] = gridSquares[0][numCols - 1];  // Top right
                            

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

                        Location spawnLocation = unoccupiedCorners.get(spawnLocationIdx);

                        // Tell the corner that it is occupied, and tell the robot its coordinates
                        spawnLocation.setRobot(nextRobot);
                        nextRobot.setCoordinates( spawnLocation.getCoordinates() );
                        
                        // If there is a wall on the spawn point, damage it
                        FortressWall wallOnSpawnPoint = spawnLocation.getWall();
                        if(wallOnSpawnPoint != null)
                        {
                            wallOnSpawnPoint.damage();
                        }

                        // Add the robot to the map
                        robots.put(nextRobot.getId(), nextRobot);

                        //Save the coordinates to print to the screen 
                        Vector2d spawnCoords = nextRobot.getCoordinates();

                                               
                        //Start the robot, and store a reference to its execution in the map (so it can be interrupted later)
                        Future<?> f = robotExecutorService.submit(nextRobot);
                        robotFutures.put( nextRobot.getId(), f );

                        
                        // Redraw JFXArena UI element, and log robot spawn on screen
                        Platform.runLater( () -> {
                            app.log("Spawned robot '" + nextRobot.getId() + "' at " + spawnCoords.toString() + "\n");
                        } );

                        updateUi();                        
                    }                    
                }  
            }
            catch(InterruptedException iE)
            {
                //TODO
            }

        };
    }

    private Runnable wallSpawnConsumerRunnable()
    {
        return () -> {
            try
            {
                while(true)
                {
                    FortressWall newWall = wallSpawnBlockingQueue.take();

                    Vector2d wallPos = newWall.getCoordinates();

                    int wallX = (int)wallPos.x(); // Note: Disregards fractional position. Shouldn't matter if called appropriately
                    int wallY = (int)wallPos.y(); // Same as above

                    synchronized(gameStateMutex)
                    {                        
                        Location location = gridSquares[wallX][wallY];

                        // If this wall replaces an existing wall, remove the old wall
                        FortressWall previousWall = location.getWall();
                        if(previousWall != null)
                        {
                            destroyWall(previousWall);
                        }

                        location.setWall(newWall); // Note: If a wall already exists, this assumes a new wall can be placed to "refresh" it (e.g. if it was damamged)
                        placedWalls.add(newWall);
                    }
                   
                    Platform.runLater(() -> {
                        app.log("Spawned wall at (" + wallX + ", " + wallY + ")\n");
                    });

                    updateUi();
                }
            }
            catch(InterruptedException iE)
            {

            }
        };
    }    

    // Adds a new robot to the blocking queue 
    // Thread: Runs in the calling thread (usually the Robot-Spawn-Producer thread)
    public void putNewRobot(Robot robot) throws InterruptedException
    {
        this.robotSpawnBlockingQueue.put(robot);
    }
   

    /*
     * Allows a robot to request to make a move
     * 
     * If move is valid
     * 
     * Thread: Robot's thread
     */
    public boolean requestMove(RobotMoveRequest request) throws InterruptedException
    {
        Robot robot = request.getRobot();
        Vector2d startPos = robot.getCoordinates();
        Vector2d endPos = startPos.plus( request.getMove() );

        int startX = (int)startPos.x(); // Note: Disregards robot's fractional position. Shouldn't matter if called appropriately
        int startY = (int)startPos.y(); // Same as above

        int endX = (int)endPos.x(); // Same as above
        int endY = (int)endPos.y(); // Same as above

        // Out of bounds
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

            //Move is valid ----

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
     * Thread: Runs in Robot's thread
     */
    public void updateRobotPos(Robot robot, Vector2d pos)
    {
        synchronized(gameStateMutex)
        {
            robot.setCoordinates(pos);
        }

        updateUi();    
    }

    /*
     * Runs when a robot finishes its move. 
     * 
     * Frees the Location that the robot came from, and checks for Wall collisions
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
                destroyRobot(robot);
                wall.damage();
            }

            // Check for game over
            if(endLocation.hasCitadel())
            {
                gameOver();
            }

            gameStateMutex.notifyAll(); // Notify spawner that a corner might be free
        }


    }

    private void gameOver()
    {
        int finalScore = score.getScore();

        Platform.runLater( () -> {
            app.gameOver(finalScore);
        });
    }


    /*
     * Called when a robot hits a wall
     * 
     * Cancel's the robot's Future, removes the Future from the list, removes the
     * robot from the Map of robots, updates the Location, and displays a log on screen
     * 
     * Thread: Called by the robot's thread
     */
    private void destroyRobot(Robot robot)
    {
        synchronized(gameStateMutex)
        {
            int id = robot.getId();

            // Interrupt the robot's task, and remove it from the map
            Future<?> future = robotFutures.get(id);
            future.cancel(true);

            robotFutures.remove(id);

            // Remove the robot from its location
            int x = (int)robot.getCoordinates().x(); // Ignores fractional part of coordinate. Shouldn't matter if called appropriately
            int y = (int)robot.getCoordinates().y(); // Same as above

            Location location = gridSquares[x][y];
            location.setRobot(null);

            // Remove the robot from the list of active robots
            robots.remove(id);

            // Increase the score
            score.robotDestroyed();

            // Show the message on screen
            String msg = "Robot '" + id + "' hit a wall at (" + x + ", " + y + ")\n";
            Platform.runLater(() -> {
                app.log(msg);
            });
        }

        

        updateUi();
    }

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

        updateUi();
    }


    /*
     * Returns a List of all robots in the game (as ReadOnlyRobots)
     * 
     * Thread: Runs in the calling thread (typically UI)
     */
    public List<ReadOnlyRobot> getRobots()
    {
        List<ReadOnlyRobot> list = new ArrayList<>();

        synchronized(gameStateMutex)
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

        synchronized(gameStateMutex)
        {
            for(FortressWall w : placedWalls)
            {
                list.add( new ReadOnlyFortressWall(w));
            }

        }

        return list;
    }

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
        wallSpawner.requestWall(x, y);
    }

    /*
     * Returns the number of "placed" walls. This actaully returns the number of the walls 
     * that have been placed, plus the number of walls pending placement by the consumer thread
     */
    public int getNumSpawnedWalls()
    {
        return wallSpawnBlockingQueue.size() + placedWalls.size();
    }

    public void putNewWall(FortressWall wall) throws InterruptedException
    {
        wallSpawnBlockingQueue.put(wall);
    }


    public void updateUi()
    {
        Platform.runLater( () -> {
            arena.requestLayout();
        } );  
    }
}
