package edu.curtin.saed.assignment1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.curtin.saed.assignment1.entities.robot.*;
import edu.curtin.saed.assignment1.entities.fortress_wall.*;
import edu.curtin.saed.assignment1.entities.fortress_wall.FortressWall;
import edu.curtin.saed.assignment1.entities.robot.Robot;
import edu.curtin.saed.assignment1.misc.Coordinates;
import edu.curtin.saed.assignment1.misc.Location;
import edu.curtin.saed.assignment1.misc.RobotSpawner;
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
    private Thread robotMoveValidatorThread;   

    // BLOCKING QUEUES
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(4);
    private BlockingQueue<FortressWall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(20);
    //private BlockingQueue<...> robotMoveBlockingQueue = new ArrayBlockingQueue<>(...);

    // GAME STATE INFO
    private Location[][] gridSquares; // Accessed by robotSpawnConsumerThread, wallSpawnConsumerThread and robotMoveValidatorThread.
    private List<Thread> robotThreads = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread  TODO - used by robotMoveValidatorThread for robot destruction on wall impact callbacks?
    private List<Robot> robots = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread, robotMoveValidatorThread
    private List<FortressWall> walls = Collections.synchronizedList(new ArrayList<>()); // Accessed by robotSpawnConsumerThread, robotMoveValidatorThread

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

        //TODO Fix need for this...
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

    private void initGridSquares(int numRows, int numCols)
    {
        //Initialise array
        this.gridSquares = new Location[numRows][numCols];

        for(int i = 0; i < numRows; i++)
        {
            for(int j = 0; j < numCols; j++)
            {
                this.gridSquares[i][j] = new Location( new Coordinates(j, i) );
            }
        }

        //Set the citadel in the middle square. If even rows, favour row under middle; if even cols, facour col right of middle.
        int middleRow = (numRows / 2) + 1;
        int middleCol = (numCols / 2) + 1;

        this.gridSquares[middleRow][middleCol].setCitadel(true);
    }


    public void start()
    {
        if(robotSpawnConsumerThread != null || wallSpawnConsumerThread != null || robotMoveValidatorThread != null)
        {
            throw new IllegalStateException("Can't start a GameEngine that is already running.");
        }

        robotSpawnConsumerThread = new Thread(robotSpawnConsumerRunnable(), "robot-spawner");
        wallSpawnConsumerThread = new Thread(wallSpawnConsumerRunnable(), "wall-spawner");
        robotMoveValidatorThread = new Thread(robotMoveValidatorRunnable(), "robot-mover");
        robotSpawnProducerThread = new Thread( new RobotSpawner(this) );

        robotSpawnConsumerThread.start();
        wallSpawnConsumerThread.start();
        robotMoveValidatorThread.start();
        robotSpawnProducerThread.start();
    }
    
    public void stop()
    {
        if(robotSpawnConsumerThread == null || wallSpawnConsumerThread == null || robotMoveValidatorThread == null)
        {
            throw new IllegalStateException("Can't stop a GameEngine that hasn't started.");
        }

        robotSpawnConsumerThread.interrupt();
        wallSpawnConsumerThread.interrupt();
        robotMoveValidatorThread.interrupt();
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
                        Coordinates spawnCoords = nextRobot.getCoordinates();

                        // Add the robot's thread to the list of threads, and start it
                        String threadName = "robot-" + nextRobot.getId();
                        Thread robotThread = new Thread(nextRobot, threadName); //TODO Thread pool
                        robotThreads.add(robotThread); //TODO Synchronise robotThreads list separately to gridsquares?
                        robotThread.start();

                        
                        // TODO Notify JFXArena to redraw UI (??)
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

    private Runnable robotMoveValidatorRunnable()
    {
        return () -> {

        };
    }

    public void putNewRobot(Robot robot) throws InterruptedException
    {
        this.robotSpawnBlockingQueue.put(robot);
    }

    public List<ReadOnlyRobot> getRobots()
    {
        List<ReadOnlyRobot> list = new ArrayList<>();

        synchronized(gameStateMutex)
        {
            for(Robot r : this.robots)
            {
                list.add( new ReadOnlyRobot(r));
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


}
