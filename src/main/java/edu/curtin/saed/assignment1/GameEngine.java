package edu.curtin.saed.assignment1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.curtin.saed.assignment1.entities.*;
import edu.curtin.saed.assignment1.misc.Coordinates;
import edu.curtin.saed.assignment1.misc.Location;

public class GameEngine 
{
    // GAME_ENGINE THREADS
    private Thread robotSpawnConsumerThread;
    private Thread wallSpawnConsumerThread;
    private Thread robotMoveValidatorThread;   

    // BLOCKING QUEUES
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(4);
    private BlockingQueue<FortressWall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(20);
    //private BlockingQueue<...> robotMoveBlockingQueue = new ArrayBlockingQueue<>(...);

    // GAME STATE INFO
    private Location[][] gridSquares; // Accessed by robotSpawnConsumerThread, wallSpawnConsumerThread and robotMoveValidatorThread
    private List<Thread> robotThreads = new ArrayList<>(); // Accessed by robotSpawnConsumerThread  TODO - used by robotMoveValidatorThread for robot destruction on wall impact callbacks?

    private final int numRows;
    private final int numCols;

    // MUTEXES
    private Object gridSquaresMutex = new Object(); // Used to lock "gridSquares"

    // MISC
    Random rand = new Random();

    //CONSTRUCTOR
    public GameEngine(int numRows, int numCols)
    {
        if(numRows < 3)
        {
            throw new IllegalStateException("GameEngine only support grids with at least 3 rows.");
        }

        if(numCols > 3)
        {
            throw new IllegalStateException("GameEngine only support grids with at least 3 cols.");
        }

        this.numRows = numRows;
        this.numCols = numCols;

        initGridSquares(numRows, numCols);
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
                    
                    synchronized(gridSquaresMutex)
                    {
                        // Get all four corners
                        Location[] corners = { 
                            gridSquares[0][0],  // Top left
                            gridSquares[numRows - 1][0], // Bottom left
                            gridSquares[numRows - 1][numCols - 1], //Bottom right
                            gridSquares[0][numCols - 1]  // Top right
                        };
                        

                        // Add corner Locations that are unoccupied by other robots to a list
                        List<Location> unoccupiedCorners = new ArrayList<>();
                        for(Location l : corners)
                        {                            
                            if(l.getRobot() == null)
                            {
                                unoccupiedCorners.add(l);
                            }
                        }

                        // Place the robot in the grid
                        if(unoccupiedCorners.size() == 0)
                        {
                            //TODO
                        }  
                        else
                        {
                            // Randomly choose one of the unoccupied corners as the robot's spawn point
                            int spawnLocationIdx = rand.nextInt(0, unoccupiedCorners.size() - 1);

                            Location spawnLocation = corners[spawnLocationIdx];

                            // Tell the corner that it is occupied, and tell the robot its coordinates
                            spawnLocation.setRobot(nextRobot);
                            nextRobot.setCoordinates( spawnLocation.getCoordinates() );
                        }


                        // Add the robot's thread to the list of threads, and start it
                        String threadName = "robot-" + nextRobot.getId();
                        Thread robotThread = new Thread(nextRobot, threadName); //TODO Thread pool
                        robotThreads.add(robotThread); //TODO Synchronise robotThreads list separately to gridsquares?
                        robotThread.start();

                        // TODO Notify JFXArena to redraw UI
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

}
