package edu.curtin.saed.assignment1.misc;

import edu.curtin.saed.assignment1.entities.fortress_wall.FortressWall;
import edu.curtin.saed.assignment1.entities.robot.Robot;

public class Location 
{
    private final Coordinates coordinates;
    private Robot robot;
    private FortressWall wall;
    private boolean citadel;

    public Location(Coordinates coordinates)
    {
        this.coordinates = coordinates;
        this.robot = null;
        this.wall = null;
        this.citadel = false;
    }

    // ACCESSORS ----------------------------
    public Robot getRobot()
    {
        return this.robot;
    }

    public FortressWall getWall()
    {
        return this.wall;
    }

    public boolean hasCitadel()
    {
        return this.citadel;
    }

    // MUTATORS ----------------------------------
    public void setRobot(Robot newRobot)
    {
        if(this.robot != null)
        {
            throw new IllegalStateException("Can't set robot on an already occupied Location");
        }

        this.robot = newRobot;
    }

    public void removeRobot()
    {
        this.robot = null;
    }

    public void setWall(FortressWall newWall)
    {
        if(this.wall != null)
        {
            throw new IllegalStateException("Can't set wall on an already occupied Location");
        }

        this.wall = newWall;
    }

    public void removeWall()
    {
        this.wall = null;
    }

    public void setCitadel(boolean citadel)
    {
        this.citadel = citadel;
    }

    //Return a copy of this location's coordinates
    public Coordinates getCoordinates()
    {
        return new Coordinates(this.coordinates);
    }
    


}
