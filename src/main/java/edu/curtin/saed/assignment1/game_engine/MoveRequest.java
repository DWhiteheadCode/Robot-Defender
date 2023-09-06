package edu.curtin.saed.assignment1.game_engine;

import edu.curtin.saed.assignment1.entities.robot.Robot;
import edu.curtin.saed.assignment1.misc.Vector2d;

public class MoveRequest 
{
    private Robot robot;
    private Vector2d move;

    public MoveRequest(Robot robot, Vector2d move)
    {
        if(move.size() > 1)
        {
            throw new IllegalArgumentException("Can't make a move that is more than 1 unit long");
        }

        this.robot = robot;
        this.move = move;
    }

    public Robot getRobot()
    {
        return this.robot;
    }

    public Vector2d getMove()
    {
        return new Vector2d(move);
    }
    

}
