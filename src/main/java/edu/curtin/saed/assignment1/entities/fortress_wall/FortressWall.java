package edu.curtin.saed.assignment1.entities.fortress_wall;

import edu.curtin.saed.assignment1.game_engine.GameEngine;
import edu.curtin.saed.assignment1.misc.Vector2d;

public class FortressWall 
{
    public static final String UNDAMAGED_IMAGE_FILE = "181478.png";
    public static final String DAMAGED_IMAGE_FILE = "181479.png";

    private Vector2d coordinates;
    private boolean isDamaged;

    private GameEngine gameEngine;

    public FortressWall(GameEngine gameEngine, Vector2d coordinates)
    {
        this.gameEngine = gameEngine;
        this.isDamaged = false;
        this.coordinates = coordinates;
    }

    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }

    public boolean isDamaged()
    {
        return isDamaged;
    }

    public void damage()
    {
        if(isDamaged)
        {
            destroy();
            return;
        }      
        
        this.isDamaged = true;
        gameEngine.updateArenaUi();
    }

    private void destroy()
    {
        gameEngine.destroyWall(this);
    }

}
