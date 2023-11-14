package dwhiteheadcode.com.github.robot_defender.misc;

import dwhiteheadcode.com.github.robot_defender.entities.fortress_wall.FortressWall;
import dwhiteheadcode.com.github.robot_defender.entities.robot.Robot;

/*
 * Represents a location on the game board (i.e. a square in the grid) 
 * 
 * GameEngine is responsible for locking mutex(es) related to Locations, and preventing misuse
 */
public class Location 
{
    private final Vector2d coordinates; // Coordinates of the Location
    private Robot robot; // The robot that occupies the location (could be null)
    private FortressWall wall; // The wall that occupies the location (could be null)
    private boolean citadel; // Whether or not this Location has the citadel

    public Location(Vector2d coordinates)
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
        if(this.robot != null && newRobot != null) // Allows newRobot = null to clear this robot
        {
            throw new IllegalStateException("Can't set robot on an already occupied Location");
        }

        this.robot = newRobot;
    }

    public void setWall(FortressWall newWall)
    {
        if(this.wall != null && newWall != null) // Allows newWall = nul to clear this wall
        {
            throw new IllegalStateException("Can't set wall on an already occupied Location"); 
        }   

        this.wall = newWall;
    }

    public void setCitadel(boolean citadel)
    {
        this.citadel = citadel;
    }

    //Return a copy of this location's coordinates
    public Vector2d getCoordinates()
    {
        return new Vector2d(this.coordinates);
    }
    


}
