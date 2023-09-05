package edu.curtin.saed.assignment1.entities.robot;

import edu.curtin.saed.assignment1.misc.Coordinates;

public class ReadOnlyRobot
{
    private final int id;
    private Coordinates coordinates;
    
    public ReadOnlyRobot(Robot robot)
    {
        this.id = robot.getId();
        this.coordinates = robot.getCoordinates();
    }

    public int getId()
    {
        return this.id;
    }

    public Coordinates getCoordinates()
    {
        return new Coordinates(this.coordinates);
    }


}
