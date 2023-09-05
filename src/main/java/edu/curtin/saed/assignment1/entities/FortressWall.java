package edu.curtin.saed.assignment1.entities;

import edu.curtin.saed.assignment1.GameEngine;
import edu.curtin.saed.assignment1.misc.Coordinates;

public class FortressWall 
{
    private Coordinates coordinates;
    private boolean isDamaged;

    private GameEngine gameEngine;

    public FortressWall(GameEngine gameEngine)
    {
        this.gameEngine = gameEngine;
        this.isDamaged = false;
        this.coordinates = null;
    }

    public void setCoordinates(Coordinates coordiantes)
    {
        this.coordinates = coordiantes;
    }

    public Coordinates getCoordinates()
    {
        return new Coordinates(this.coordinates);
    }

    public boolean isDamaged()
    {
        return isDamaged;
    }

}
