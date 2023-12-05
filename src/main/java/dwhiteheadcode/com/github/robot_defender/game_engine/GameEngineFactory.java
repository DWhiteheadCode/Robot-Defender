package dwhiteheadcode.com.github.robot_defender.game_engine;

import dwhiteheadcode.com.github.robot_defender.GameWindow;
import dwhiteheadcode.com.github.robot_defender.game_engine.components.FortressWallSpawner;
import dwhiteheadcode.com.github.robot_defender.game_engine.components.RobotSpawner;
import dwhiteheadcode.com.github.robot_defender.game_engine.components.ScoreCalculator;

public class GameEngineFactory 
{
    public static final int NUM_ROWS_DEFAULT = 9;
    public static final int NUM_COLS_DEFAULT = 9;
    public static final int MAX_WALLS_DEFAULT = 10;

    public static GameEngine instance(GameWindow window)
    {
        RobotSpawner robotSpawner = new RobotSpawner();
        FortressWallSpawner wallSpawner = new FortressWallSpawner(window, MAX_WALLS_DEFAULT);
        ScoreCalculator scoreCalculator = new ScoreCalculator(window);

        GameEngine engine = new GameEngine(window, NUM_ROWS_DEFAULT, NUM_COLS_DEFAULT, MAX_WALLS_DEFAULT, 
            robotSpawner, wallSpawner, scoreCalculator);

        robotSpawner.setGameEngine(engine);
        wallSpawner.setGameEngine(engine);

        return engine;
    }


}
