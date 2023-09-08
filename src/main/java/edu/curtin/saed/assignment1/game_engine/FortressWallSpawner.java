package edu.curtin.saed.assignment1.game_engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.curtin.saed.assignment1.entities.fortress_wall.FortressWall;
import edu.curtin.saed.assignment1.misc.Vector2d;

public class FortressWallSpawner implements Runnable
{
    private final int WALL_SPAWN_DELAY_MILLISECONDS = 2000;
    private final int MAX_NUM_WALLS = 10;

    private BlockingQueue<FortressWall> wallRequestBlockingQueue = new ArrayBlockingQueue<>(MAX_NUM_WALLS);

    private GameEngine gameEngine;

    public FortressWallSpawner(GameEngine gameEngine)
    {
        this.gameEngine = gameEngine;
    }

    /**
     * Requests a wall be added at the given coordinates
     * 
     * A wall will be added to the queue if the total number of walls 
     * (either already placed in the game, or already in the queue)
     * is less than MAX_NUM_WALLS
     * 
     * Runs in the UI thread (hence designed to never block)
     */
    public void requestWall(int x, int y)
    {
        int spawnedWalls = gameEngine.getNumSpawnedWalls();
        int totalWalls = spawnedWalls + wallRequestBlockingQueue.size();

        if(totalWalls < MAX_NUM_WALLS)
        {
            Vector2d coordinates = new Vector2d(x, y);

            wallRequestBlockingQueue.offer(new FortressWall(gameEngine, coordinates));
        }        
    }

    /*
     * Runs a loop that will take any new wall requests, and forward them to 
     * the game engine. 
     * 
     * Requests will be sent periodically, based on WALL_SPAWN_DELAY_MILLISECONDS
     */
    @Override
    public void run() 
    {
        try
        {
            while(true)
            {
                FortressWall request = wallRequestBlockingQueue.take();
                
                gameEngine.putNewWall( request );

                Thread.sleep(WALL_SPAWN_DELAY_MILLISECONDS);
            }
        }
        catch(InterruptedException iE)
        {

        }



    }
    
}
