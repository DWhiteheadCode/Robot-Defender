package edu.curtin.saed.assignment1.entities;

public class Location 
{
    private Robot robot;
    private Wall wall;
    private final boolean citadel;

    public Location(boolean citadel)
    {
        this.robot = null;
        this.wall = null;
        this.citadel = citadel;
    }

    // ACCESSORS ----------------------------
    public Robot getRobot()
    {
        return this.robot;
    }

    public Wall getWall()
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

    public void setWall(Wall newWall)
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

    


}
