package edu.curtin.saed.assignment1.entities.fortress_wall;

import edu.curtin.saed.assignment1.misc.Coordinates;

public class ReadOnlyFortressWall 
{
    private Coordinates coordinates;
    private boolean isDamaged;

    public ReadOnlyFortressWall(FortressWall fortressWall)
    {
        this.coordinates = fortressWall.getCoordinates();
        this.isDamaged = fortressWall.isDamaged();
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
