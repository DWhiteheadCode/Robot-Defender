package dwhiteheadcode.com.github.robot_defender.entities.fortress_wall;

import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

/*
 * Represents a read-only version of a FortressWall, which is useful to prevent
 * the need to lock the mutex relating to the original wall. 
 */
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
