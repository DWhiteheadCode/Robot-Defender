package edu.curtin.saed.assignment1;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.curtin.saed.assignment1.entities.*;
import edu.curtin.saed.assignment1.misc.Location;

public class GameEngine 
{
    // GAME_ENGINE THREADS
    private Thread robotSpawnConsumerThread;
    private Thread wallSpawnConsumerThread;
    private Thread robotMoveValidatorThread;

    // BLOCKING QUEUES
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(4);
    private BlockingQueue<Wall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(20);
    //private BlockingQueue<...> robotMoveBlockingQueue = new ArrayBlockingQueue<>(...);

    // GAME STATE INFO
    private Location[][] gridSquares;
    private List<Thread> robotThreads;



    //CONSTRUCTOR
    public GameEngine(int numRows, int numCols)
    {
        gridSquares = new Location[numRows][numCols];
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
        return () -> {
            
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



}
