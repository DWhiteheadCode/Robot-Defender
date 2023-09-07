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
import edu.curtin.saed.assignment1.JFXArena;
import edu.curtin.saed.assignment1.entities.fortress_wall.*;
import edu.curtin.saed.assignment1.misc.Vector2d;
import edu.curtin.saed.assignment1.misc.Location;
import javafx.application.Platform;

public class GameEngine 
{
    // UI
    private App app;
    private JFXArena arena;

    // GAME_ENGINE THREADS
    private Thread robotSpawnProducerThread;
    private Thread robotSpawnConsumerThread;
    private Thread wallSpawnConsumerThread;  

    // THREAD POOL
    private ExecutorService robotExecutorService;

    // BLOCKING QUEUES
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(4);
    private BlockingQueue<FortressWall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(20);

    // GAME STATE INFO - All except "citadel" is considered the same resource, and is locked with gameStateMutex
    private Location[][] gridSquares; // Accessed by robotSpawnConsumerThread, wallSpawnConsumerThread and robot threads.
    private Map<Integer, Future<?>> robotFutures = Collections.synchronizedMap( new HashMap<>() ); // TODO accessed by which threads? Needs synchronized?
    private List<Robot> robots = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread, robotMoveValidatorThread
    private List<FortressWall> walls = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread, robotMoveValidatorThread
    
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
        // Min 4 threads, max threads = numSquares - 1 (this is the maximum number of robots)
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
        if(robotSpawnConsumerThread != null || wallSpawnConsumerThread != null)
        {
            throw new IllegalStateException("Can't start a GameEngine that is already running.");
        }

        robotSpawnConsumerThread = new Thread(robotSpawnConsumerRunnable(), "robot-spawn-consumer");
        wallSpawnConsumerThread = new Thread(wallSpawnConsumerRunnable(), "wall-spawn-consumer");
        robotSpawnProducerThread = new Thread( new RobotSpawner(this), "robot-spawn-producer" );

        robotSpawnConsumerThread.start();
        wallSpawnConsumerThread.start();
        robotSpawnProducerThread.start();
    }
    
    public void stop()
    {
        if(robotSpawnConsumerThread == null || wallSpawnConsumerThread == null)
        {
            throw new IllegalStateException("Can't stop a GameEngine that hasn't started.");
        }

        robotSpawnConsumerThread.interrupt();
        wallSpawnConsumerThread.interrupt();
        robotSpawnProducerThread.interrupt();
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
                        
                        // Add the robot to the list of robots
                        robots.add(nextRobot);

                        //Save the coordinates to print to the screen 
                        Vector2d spawnCoords = nextRobot.getCoordinates();

                                               
                        //Start the robot, and store a reference to its execution in the map (so it can be interrupted later)
                        Future<?> f = robotExecutorService.submit(nextRobot);
                        robotFutures.put( nextRobot.getId(), f );

                        
                        // TODO Redraw JFXArena UI element, and log robot spawn on screen
                        Platform.runLater( () -> {
                            app.log("Spawned robot at " + spawnCoords.toString() + "\n");
                            this.arena.requestLayout();
                        } );
                        
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
    public boolean requestMove(MoveRequest request) throws InterruptedException
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

        Platform.runLater( () -> {
            arena.requestLayout();
        } );        
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

            // Check for game over
            if(endLocation.hasCitadel())
            {
                //TODO Gameover
            }

            //Check for wall collision
            FortressWall wall = endLocation.getWall();
            if( wall != null)
            {
                robot.destroy();
                wall.damage();
            }

            gameStateMutex.notifyAll(); // Notify spawner that a corner might be free
        }


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
            for(Robot r : this.robots)
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
    public List<ReadOnlyFortressWall> getWalls()
    {
        List<ReadOnlyFortressWall> list = new ArrayList<>();

        synchronized(gameStateMutex)
        {
            for(FortressWall w : walls)
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

}
