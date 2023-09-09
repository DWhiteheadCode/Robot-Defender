package edu.curtin.saed.assignment1.game_engine;

import edu.curtin.saed.assignment1.entities.robot.Robot;

/*
 * A class that produces Robots, and gives them to GameEngine for use in the game. 
 */
public class RobotSpawner implements Runnable
{
    private static final long ROBOT_SPAWN_DELAY_MILLISECONDS = 1500;
    private static final long ROBOT_INITIAL_SPAWN_DELAY = 500;
    
    private GameEngine gameEngine;
    private int robotCount = 0; // Tracks the number of robots created by this spawner. Used for robot.id
   
    public RobotSpawner(GameEngine gameEngine)
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
        try
        {
            Thread.sleep(ROBOT_INITIAL_SPAWN_DELAY); // Added so a robot doesn't spawn before the window is displayed for the first time
         
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
            // Nothing needed here.
        }
    }
    
}
