package edu.curtin.saed.assignment1.game_engine;

public class ScoreCalculator implements Runnable
{
    private final int SCORE_DELAY_MILLISECONDS = 1000;
    private final int PASSIVE_SCORE_INCREMENT = 10;
    private final int ROBOT_DESTROYED_SCORE = 100;

    private int score = 0;

    private Object mutex = new Object();

    private GameEngine gameEngine;

    public ScoreCalculator(GameEngine gameEngine)
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
                synchronized(mutex)
                {
                    score += PASSIVE_SCORE_INCREMENT;
                    gameEngine.updateScore(score);
                }

                Thread.sleep(SCORE_DELAY_MILLISECONDS);
            }
        }
        catch(InterruptedException iE)
        {

        }
    }
    
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
