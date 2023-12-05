package dwhiteheadcode.com.github.robot_defender.game_engine.components;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.time.Duration;

import dwhiteheadcode.com.github.robot_defender.GameWindow;
import dwhiteheadcode.com.github.robot_defender.entities.fortress_wall.FortressWall;
import dwhiteheadcode.com.github.robot_defender.game_engine.GameEngine;
import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

/*
 * A class for spawning FortressWalls, for use by a GameEngine.
 */
public class FortressWallSpawner implements Runnable
{
    private static final Duration WALL_SPAWN_DELAY = Duration.ofMillis(2000); // The delay after building 1 wall before the next can be built
    private static final Duration WALL_COOLDOWN_UPDATE_INTERVAL = Duration.ofMillis(100); // The amount of time to wait before updating UI about the cooldown
    
    private BlockingQueue<FortressWall> wallRequestBlockingQueue;

    private GameEngine gameEngine;
    private GameWindow gameWindow;
    private int maxWalls;

    public FortressWallSpawner(GameWindow gameWindow, int maxWalls)
    {
        this.maxWalls = maxWalls;
        this.gameWindow = gameWindow;
        this.wallRequestBlockingQueue = new ArrayBlockingQueue<>(maxWalls);
    }

    public void setGameEngine(GameEngine gameEngine)
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
        if(this.gameEngine == null)
        {
            throw new IllegalStateException("FortressWallSpawner's GameEngine must be set before it can be started.");
        }

        try
        {
            while(true)
            {
                FortressWall request = wallRequestBlockingQueue.take();
                gameEngine.putNewWall( request );               
                cooldown();                
            }
        }
        catch(InterruptedException iE)
        {
            // Nothing needed here.
        }
    }

    /*
     * Sleeps the calling thread for a total duration of WALL_SPAWN_DELAY. Periodically (specifically, 
     * every WALL_COOLDOWN_UPDATE_INTERVAL), gameWindow's "cooldown" text is updated to reflect the
     * remaining cooldown duration.
     */
    private void cooldown() throws InterruptedException
    {
        long numUpdates = WALL_SPAWN_DELAY.toMillis() / WALL_COOLDOWN_UPDATE_INTERVAL.toMillis();
        long remainingCooldownMillis = WALL_SPAWN_DELAY.toMillis();

        for(long i = 0; i < numUpdates; i++)
        {
            gameWindow.setWallCooldownText( remainingCooldownMillis );
            Thread.sleep( WALL_COOLDOWN_UPDATE_INTERVAL.toMillis() );
            remainingCooldownMillis -= WALL_COOLDOWN_UPDATE_INTERVAL.toMillis();
        }

        gameWindow.setWallCooldownText(0);
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

        if(totalWalls < this.maxWalls)
        {
            Vector2d coordinates = new Vector2d(x, y);

            wallRequestBlockingQueue.offer(new FortressWall(gameEngine, gameWindow, coordinates));
            gameEngine.updateAvailableWallsText();
        }        
    }

    public int queueSize()
    {
        return wallRequestBlockingQueue.size();
    }
    
}
