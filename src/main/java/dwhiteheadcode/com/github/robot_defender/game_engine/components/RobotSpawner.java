package dwhiteheadcode.com.github.robot_defender.game_engine.components;

import java.time.Duration;

import dwhiteheadcode.com.github.robot_defender.entities.robot.Robot;
import dwhiteheadcode.com.github.robot_defender.game_engine.GameEngine;

/*
 * A class that produces Robots, and gives them to GameEngine for use in the game. 
 */
public class RobotSpawner implements Runnable
{
    private static final Duration ROBOT_SPAWN_DELAY = Duration.ofMillis(1500);
    
    private GameEngine gameEngine;
    private int robotCount = 0; // Tracks the number of robots created by this spawner. Used for robot.id
   
    
    public void setGameEngine(GameEngine gameEngine)
    {
        this.gameEngine = gameEngine;
    }


    /*
     * Runs a loop that creates new robots and gives them to the game engine.
     * 
     * Robots are created periodically, based on ROBOT_SPAWN_DELAY_MILLISECONDS
     */
    @Override
    public void run() 
    {
        if(this.gameEngine == null)
        {
            throw new IllegalStateException("RobotSpawner's GameEngine must be set before it can be started.");
        }

        try
        {
            while(true)
            {
                Thread.sleep(ROBOT_SPAWN_DELAY.toMillis());

                this.robotCount++;
                Robot robot = new Robot(robotCount, gameEngine);
                
                this.gameEngine.putNewRobot(robot);
            }
        }
        catch(InterruptedException iE)
        {
            // Nothing needed here.
        }
    }
    
}
