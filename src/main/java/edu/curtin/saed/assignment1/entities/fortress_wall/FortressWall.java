package edu.curtin.saed.assignment1.entities.fortress_wall;

import edu.curtin.saed.assignment1.game_engine.GameEngine;
import edu.curtin.saed.assignment1.misc.Vector2d;

public class FortressWall 
{
    public static final String UNDAMAGED_IMAGE_FILE = "181478.png";
    public static final String DAMAGED_IMAGE_FILE = "181479.png";

    private Vector2d coordinates;
    private boolean isDamaged; // Not locked because GameEngine prevents multiple robots from colliding with the wall at the same time.               

    private GameEngine gameEngine;

    public FortressWall(GameEngine gameEngine, Vector2d coordinates)
    {
        this.gameEngine = gameEngine;
        this.isDamaged = false;
        this.coordinates = coordinates;
    }

    /*
     * Returns a copy of this Wall's coordinates
     * 
     * These are never modified, and so don't need to be synchronized
     * 
     * Thread: Called by wall-spawn-consumer thread, or robot thread
     */
    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }

    /*
     * Returns false if the wall is not damaged, and true if it is.
     * 
     * Thread: Called by UI thread. GameEngine is responsible for locking this.
     */
    public boolean isDamaged()
    {
        return isDamaged;
    }

    /*
     * Called when a robot hits this wall.
     * 
     * If the wall was not damaged prior to this call, it becomes damaged.
     * If it was damaged prior to this call, it tells the gameEngine that it needs to be destroyed
     * 
     * Thread: Called by a robot's thread.
     */
    public void damage()
    {
        if(isDamaged)
        {
            gameEngine.destroyWall(this);
        }
        else
        {
            this.isDamaged = true;
            gameEngine.updateArenaUi();
        }      
    }

}
