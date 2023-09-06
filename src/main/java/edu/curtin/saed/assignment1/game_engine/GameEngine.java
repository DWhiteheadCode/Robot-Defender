package edu.curtin.saed.assignment1.game_engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.curtin.saed.assignment1.entities.robot.*;
import edu.curtin.saed.assignment1.App;
import edu.curtin.saed.assignment1.JFXArena;
import edu.curtin.saed.assignment1.entities.fortress_wall.*;
import edu.curtin.saed.assignment1.entities.fortress_wall.FortressWall;
import edu.curtin.saed.assignment1.entities.robot.Robot;
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

    // BLOCKING QUEUES
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(4);
    private BlockingQueue<FortressWall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(20);
    //private BlockingQueue<...> robotMoveBlockingQueue = new ArrayBlockingQueue<>(...);

    // GAME STATE INFO - All except "citadel" is a single resource
    private Location[][] gridSquares; // Accessed by robotSpawnConsumerThread, wallSpawnConsumerThread and robotMoveValidatorThread.
    private List<Thread> robotThreads = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread  TODO - used by robotMoveValidatorThread for robot destruction on wall impact callbacks?
    private List<Robot> robots = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread, robotMoveValidatorThread
    private List<FortressWall> walls = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread, robotMoveValidatorThread
    
    private Vector2d citadel; // Only used by constructor thread, and robotMoveValidatorThread, so does not need to be locked.

    private final int numRows;
    private final int numCols;

    // MUTEXES
    private Object gameStateMutex = new Object(); // Used to lock "gridSquares", "robots", "walls" TODO and robotThreads?

    // MISC
    Random rand = new Random();

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

        //TODO Fix the need for this...
        this.app = app;

        this.numRows = numRows;
        this.numCols = numCols;

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
     */
    private void initGridSquares(int numRows, int numCols)
    {
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
        int middleRow = (numRows / 2) + 1;
        int middleCol = (numCols / 2) + 1;

        this.gridSquares[middleRow][middleCol].setCitadel(true);

        this.citadel = new Vector2d(middleCol, middleRow);
    }


    public void start()
    {
        if(robotSpawnConsumerThread != null || wallSpawnConsumerThread != null)
        {
            throw new IllegalStateException("Can't start a GameEngine that is already running.");
        }

        robotSpawnConsumerThread = new Thread(robotSpawnConsumerRunnable(), "robot-spawner");
        wallSpawnConsumerThread = new Thread(wallSpawnConsumerRunnable(), "wall-spawner");
        robotSpawnProducerThread = new Thread( new RobotSpawner(this) );

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
            try
            {
                while(true)
                {
                    Robot nextRobot = robotSpawnBlockingQueue.take();

                    synchronized(gameStateMutex)
                    {
                        Location[] corners = new Location[4];
                        List<Location> unoccupiedCorners;

                        // Wait until there is at least 1 free corner
                        do
                        {
                            //Get the Locations from the 4 corners of the map
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

                            // Release the mutex until something in the grid changes, possibly freeing a corner
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
                            spawnLocationIdx = rand.nextInt( 0, (unoccupiedCorners.size() -1) );
                        }

                        Location spawnLocation = unoccupiedCorners.get(spawnLocationIdx);

                        // Tell the corner that it is occupied, and tell the robot its coordinates
                        spawnLocation.setRobot(nextRobot);
                        nextRobot.setCoordinates( spawnLocation.getCoordinates() );
                        
                        // Add the robot to the list of robots
                        robots.add(nextRobot);

                        //Save the coordinates to print to the screen 
                        Vector2d spawnCoords = nextRobot.getCoordinates();

                        // Add the robot's thread to the list of threads, and start it
                        String threadName = "robot-" + nextRobot.getId();
                        Thread robotThread = new Thread(nextRobot, threadName); //TODO Thread pool
                        robotThreads.add(robotThread); //TODO Synchronise robotThreads list separately to gridsquares?
                        robotThread.start();
                        
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
     * Called when a robot wants to update its position
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

        }


    }

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
