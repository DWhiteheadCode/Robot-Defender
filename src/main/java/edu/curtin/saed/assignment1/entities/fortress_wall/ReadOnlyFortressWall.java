package edu.curtin.saed.assignment1.entities.fortress_wall;

import edu.curtin.saed.assignment1.misc.Vector2d;

public class ReadOnlyFortressWall 
{
    private Vector2d coordinates;
    private boolean isDamaged;

    public ReadOnlyFortressWall(FortressWall fortressWall)
    {
        this.coordinates = fortressWall.getCoordinates();
        this.isDamaged = fortressWall.isDamaged();
    }

    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }

    public boolean isDamaged()
    {
        return isDamaged;
    }

}
