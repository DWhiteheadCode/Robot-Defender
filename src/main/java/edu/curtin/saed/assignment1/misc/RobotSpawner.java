package edu.curtin.saed.assignment1.misc;

import edu.curtin.saed.assignment1.GameEngine;
import edu.curtin.saed.assignment1.entities.Robot;

public class RobotSpawner implements Runnable
{
    private final long ROBOT_SPAWN_DELAY_MILLISECONDS = 1500;
    
    private GameEngine gameEngine;
    private int robotCount = 0;

    public RobotSpawner(GameEngine gameEngine)
    {
        this.gameEngine = gameEngine;
    }


    @Override
    public void run() 
    {
        try
        {
            while(true)
            {
                this.robotCount++;
                Robot robot = new Robot(robotCount, gameEngine);
                
                this.gameEngine.putNewRobot(robot);

                Thread.sleep(ROBOT_SPAWN_DELAY_MILLISECONDS);
            }
        }
        catch(InterruptedException iE)
        {
            //TODO
        }


    }
    
}
