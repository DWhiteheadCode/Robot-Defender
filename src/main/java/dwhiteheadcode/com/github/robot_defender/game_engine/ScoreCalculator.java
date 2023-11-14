package dwhiteheadcode.com.github.robot_defender.game_engine;

/*
 * Class used to keep track of the player's score.
 */
public class ScoreCalculator implements Runnable
{
    private static final int PASSIVE_SCORE_DELAY_MILLISECONDS = 1000; // 1 second
    private static final int PASSIVE_SCORE_INCREMENT = 10;
    private static final int ROBOT_DESTROYED_SCORE = 100;

    private int score = 0;

    private Object mutex = new Object(); // Used to lock score, as it is accessed by multiple threads

    private GameEngine gameEngine;

    public ScoreCalculator(GameEngine gameEngine)
    {
        this.gameEngine = gameEngine;
    }

    /*
     * Runs a loop that periodically updates score
     * 
     * This only represents the passive score generation, not the robot destruction score
     */
    @Override
    public void run() 
    {
        try
        {
            while(true)
            {
                synchronized(mutex)
                {
                    score += PASSIVE_SCORE_INCREMENT;
                    gameEngine.updateScore(score);
                }

                Thread.sleep(PASSIVE_SCORE_DELAY_MILLISECONDS);
            }
        }
        catch(InterruptedException iE)
        {
            // Nothing needed here
        }
    }
    
    /*
     * Called to increase the score when a robot has been destroyed
     * 
     * Thread: Robot thread (if it moved into a wall) or robot-spawn-consumer 
     *         (if the robot spawned on a wall)
     */
    public void robotDestroyed()
    {
        synchronized(mutex)
        {
            score += ROBOT_DESTROYED_SCORE;
            gameEngine.updateScore(score);
        }
    }

    public int getScore()
    {
        synchronized(mutex)
        {
            return score;
        }
    }


}
