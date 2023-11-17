package dwhiteheadcode.com.github.robot_defender.game_engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.time.Duration;

import dwhiteheadcode.com.github.robot_defender.entities.fortress_wall.FortressWall;
import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

/*
 * A class for spawning FortressWalls, for use by a GameEngine.
 */
public class FortressWallSpawner implements Runnable
{
    private static final Duration WALL_SPAWN_DELAY = Duration.ofMillis(2000); // The delay after building 1 wall before the next can be built
    private static final int MAX_NUM_WALLS = 10;

    private BlockingQueue<FortressWall> wallRequestBlockingQueue = new ArrayBlockingQueue<>(MAX_NUM_WALLS);

    private GameEngine gameEngine;

    public FortressWallSpawner(GameEngine gameEngine)
    {
        this.gameEngine = gameEngine;
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

                Thread.sleep(WALL_SPAWN_DELAY.toMillis());
            }
        }
        catch(InterruptedException iE)
        {
            // Nothing needed here.
        }
    }

    /**
     * Requests a wall be added at the coordinates (x, y)
     * 
     * A wall will be added to the queue only if the total number of walls 
     * (either already placed in the game, or already in wallRequestBlockingQueue)
     * is less than MAX_NUM_WALLS
     * 
     * Runs in the UI thread (hence designed to never block)
     */
    public void requestWall(int x, int y, Vector2d citadelPos)
    {
        // Can't place a wall on the citadel
        if(x == citadelPos.x() && y == citadelPos.y())
        {
            return;
        }

        int spawnedWalls = gameEngine.getNumSpawnedWalls();
        int totalWalls = spawnedWalls + wallRequestBlockingQueue.size();

        if(totalWalls < MAX_NUM_WALLS)
        {
            Vector2d coordinates = new Vector2d(x, y);

            wallRequestBlockingQueue.offer(new FortressWall(gameEngine, coordinates));
            gameEngine.updateAvailableWallsText();
        }        
    }

    

    public int queueSize()
    {
        return wallRequestBlockingQueue.size();
    }

    public int maxWalls()
    {
        return MAX_NUM_WALLS;
    }
    
}
