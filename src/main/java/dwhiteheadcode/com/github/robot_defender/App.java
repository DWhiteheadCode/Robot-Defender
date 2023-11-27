package dwhiteheadcode.com.github.robot_defender;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import dwhiteheadcode.com.github.robot_defender.game_engine.GameEngine;

public class App extends Application 
{
    // Constants
    private static final int NUM_ROWS = 9;
    private static final int NUM_COLS = 9;

    // UI Elements
    private ToolBar toolbar = new ToolBar();
    private Label scoreLabel = new Label();
    private Label queuedWallsLabel = new Label();
    private Label wallCooldownLabel = new Label();
    private Label availableWallsLabel = new Label();

    private SplitPane splitPane = new SplitPane();
    
    private TextArea logger = new TextArea();

    // Arena
    private JFXArena arena;

    // Game Engine   
    private GameEngine gameEngine;

    public static void main(String[] args) 
    {
        launch();        
    }
    
    @Override
    public void start(Stage stage) 
    {
        stage.setTitle("Robot Defender");              

        startNewGame();

        // Useful for debugging
        /*
        arena.addListener((x, y) ->
        {
            System.out.println("Arena click at (" + x + "," + y + ")");
        });
        */
        
        // UI Setup only needed when the first game is created
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);
        
        Scene scene = new Scene(contentPane, 800, 800);
        stage.setScene(scene);

        stage.show();
    }


    public void startNewGame()
    {
        // Create GameEngine
        this.gameEngine = new GameEngine(this, NUM_ROWS, NUM_COLS);

        // Create JFXArena
        this.arena = new JFXArena(gameEngine, NUM_ROWS, NUM_COLS);
        this.gameEngine.setArena(arena);
        this.arena.addListener(gameEngine);
        this.arena.setMinWidth(300.0);

        // Set up/Reset UI
        this.scoreLabel.setText("Score: 0");
        this.queuedWallsLabel.setText("Queued Walls: 0");
        this.availableWallsLabel.setText("Available Walls: " + GameEngine.MAX_WALLS);
        this.wallCooldownLabel.setText("Wall Placement Cooldown: READY");
        
        if(this.toolbar.getItems().isEmpty()) // If this is the first game, add the elements to the toolbar
        {
            this.toolbar.getItems().addAll(scoreLabel, queuedWallsLabel, availableWallsLabel, wallCooldownLabel);
        }        

        this.logger.clear();

        this.splitPane.getItems().clear();
        this.splitPane.getItems().addAll(arena, logger); 

        // Start Game 
        gameEngine.start();
    }




    /*
     * Stops the gameengine when the game window is closed.
     */
    @Override
    public void stop() 
    {
        gameEngine.stop();
    }

    /*
     * Display a message in the on-screen log.
     * Must be called from UI thread.
     */
    public void log(String message)
    {
        logger.appendText(message);
    }

    /*
     * Update the score on screen.
     * Must be called from UI thread.
     */
    public void setScore(int score)
    {
        this.scoreLabel.setText("Score: " + score);
    }

    /*
     * Trigger end-of-game logic.
     * Must be called from UI thread
     */
    public void gameOver(int finalScore)
    {
        gameEngine.stop();

        new GameOverWindow().display(finalScore, this);
    }

    /*
     * Update the on-screen text displaying the number of queue walls.
     * Must be called from UI thread.
     */
    public void setQueuedWalls(int numWalls)
    {
        this.queuedWallsLabel.setText("Queued Walls: " + numWalls);
    }

    /*
     * Update the on-screen text displaying the cooldown before the next wall can/will be placed
     * Must be called from UI thread.
     */
    public void setWallCooldownText(long cooldownMillis)
    {
        if(cooldownMillis < 0)
        {
            throw new IllegalArgumentException("Can't set wall cooldown text for value " + cooldownMillis + " as it is less than 0");
        }

        if(cooldownMillis == 0l)
        {
            this.wallCooldownLabel.setText("Wall Cooldown: READY");
        }
        else
        {
            double cooldownSeconds = (double)cooldownMillis / 1000;
            this.wallCooldownLabel.setText("Wall Cooldown: " + cooldownSeconds + "s");
        }
    }

    /*
     * Update the on-screen text displaying the number of walls that can still be placed.
     * Does not count queued walls.
     * Must be called from UI thread.
     */
    public void setAvailableWallsText(int availableWalls)
    {
        this.availableWallsLabel.setText("Available Walls: " + availableWalls);
    }

}
