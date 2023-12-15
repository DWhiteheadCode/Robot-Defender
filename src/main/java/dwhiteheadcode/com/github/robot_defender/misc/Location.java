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
    private Robot robot; // The robot that occupies the location (could be null if no robot is here)
    private FortressWall wall; // The wall that occupies the location (could be null if no wall is here)
    private boolean citadel; // True if this Location has the citadel, otherwise false.

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
        // If a robot occupies this location, a new robot can't be placed here.
        // setRobot(null) can be used to clear the robot from this Location.
        if(this.robot != null && newRobot != null) 
        {
            throw new IllegalStateException("Can't set robot on an already occupied Location");
        }

        this.robot = newRobot;
    }

    public void setWall(FortressWall newWall)
    {
        // If a wall occupies this location, a new wall can't be placed here.
        //      NOTE: GameEngine manages requests for new walls at Locations with existing walls.
        //            See GameEngine for details on how these are handled.
        // setWall(null) can be used to clear the wall from this Location.
        if(this.wall != null && newWall != null) 
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
        return this.coordinates;
    }
    


}
