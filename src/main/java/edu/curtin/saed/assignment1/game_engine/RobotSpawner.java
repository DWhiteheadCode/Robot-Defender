package edu.curtin.saed.assignment1.game_engine;

import edu.curtin.saed.assignment1.entities.robot.Robot;

public class RobotSpawner implements Runnable
{
    private final long ROBOT_SPAWN_DELAY_MILLISECONDS = 1500;
    private final long ROBOT_INITIAL_SPAWN_DELAY = 500;
    
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
            Thread.sleep(ROBOT_INITIAL_SPAWN_DELAY); // Done so a robot doesn't spawn before the window is displayed for the first time

            while(true)
            {
                this.robotCount++;
                Robot robot = new Robot(robotCount, gameEngine);
                
                this.gameEngine.putNewRobot(robot);

                Thread.sleep(ROBOT_SPAWN_DELAY_MILLISECONDS); // TODO Timer/tasks? 
            }
        }
        catch(InterruptedException iE)
        {
            //TODO
        }


    }
    
}
