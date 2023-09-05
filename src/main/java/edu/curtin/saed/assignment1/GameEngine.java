package edu.curtin.saed.assignment1;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.curtin.saed.assignment1.entities.*;

public class GameEngine 
{
    // THREADS
    private Thread robotSpawnConsumerThread;
    private Thread wallSpawnConsumerThread;
    private Thread robotMoveValidatorThread;

    // BLOCKING QUEUES
    private BlockingQueue<Robot> robotSpawnBlockingQueue = new ArrayBlockingQueue<>(4);
    private BlockingQueue<Wall> wallSpawnBlockingQueue = new ArrayBlockingQueue<>(20);
    

    public void start()
    {
        if(robotSpawnConsumerThread != null || wallSpawnConsumerThread != null || robotMoveValidatorThread != null)
        {
            throw new IllegalStateException("Can't start a GameEngine that is already running.");
        }

        robotSpawnConsumerThread = new Thread(robotSpawnConsumerRunnable(), "robot-spawner");
        wallSpawnConsumerThread = new Thread(wallSpawnConsumerRunnable(), "robot-spawner");
        robotMoveValidatorThread = new Thread(robotMoveValidatorRunnable(), "robot-spawner");
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
