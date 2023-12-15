package dwhiteheadcode.com.github.robot_defender.entities.robot;

import dwhiteheadcode.com.github.robot_defender.misc.Vector2d;

/*
 * Represents a read-only version of a Robot, which is useful to prevent
 * the need to lock the mutex relating to the original robot. 
 */
public class ReadOnlyRobot
{
    private final int id;
    private Vector2d coordinates;
    
    public ReadOnlyRobot(Robot robot)
    {
        this.id = robot.getId();
        this.coordinates = robot.getCoordinates();
    }

    public int getId()
    {
        return this.id;
    }

    public Vector2d getCoordinates()
    {
        return this.coordinates;
    }


}
