package edu.curtin.saed.assignment1.entities.robot;

import edu.curtin.saed.assignment1.misc.Vector2d;

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
        return new Vector2d(this.coordinates);
    }


}
