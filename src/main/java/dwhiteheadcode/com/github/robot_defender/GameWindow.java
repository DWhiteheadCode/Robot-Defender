package dwhiteheadcode.com.github.robot_defender;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import dwhiteheadcode.com.github.robot_defender.game_engine.GameEngine;

public class GameWindow
{
    // UI Elements
    private ToolBar toolbar = new ToolBar();
    private Label scoreLabel = new Label();
    private Label queuedWallsLabel = new Label();
    private Label wallCooldownLabel = new Label();
    private Label availableWallsLabel = new Label();

    private SplitPane splitPane = new SplitPane(); // Contains game window and game log
    
    private TextArea logger = new TextArea();

    // Arena
    private JFXArena arena;

    // Game Engine   
    private GameEngine gameEngine;


    public void start(Stage stage) 
    {
        // UI Setup only needed when the first game is created
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);
        
        Scene gameScene = new Scene(contentPane, 800, 600);
        stage.setScene(gameScene);

        stage.setResizable(false);

        // Start a new game
        startNewGame();

        stage.show();
    }


    public void startNewGame()
    {
        // Create GameEngine
        this.gameEngine = GameEngine.instance(this);
        int numRows = gameEngine.getNumRows();
        int numCols = gameEngine.getNumCols();

        // Create JFXArena
        this.arena = new JFXArena(gameEngine, numRows, numCols);
        this.gameEngine.setArena(arena);
        this.arena.addListener(gameEngine);
        this.arena.setMinWidth(300.0);

        // Set up/Reset UI
        this.scoreLabel.setText("Score: 0");
        this.queuedWallsLabel.setText("Queued Walls: 0");
        this.availableWallsLabel.setText("Available Walls: " + gameEngine.getMaxWalls());
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
     * Stops the gameengine
     */
    public void stop() 
    {
        gameEngine.stop();
    }

    /*
     * Display a message in the on-screen log.
     */
    public void log(String message)
    {
        Platform.runLater( ()-> {
            logger.appendText(message);
        });        
    }

    /*
     * Update the score on screen.
     */
    public void setScore(int score)
    {
        Platform.runLater( ()-> {
            this.scoreLabel.setText("Score: " + score);
        });        
    }

    /*
     * Trigger end-of-game logic.
     */
    public void gameOver(int finalScore)
    {
        Platform.runLater( ()-> {
            gameEngine.stop();
            new GameOverWindow().display(finalScore, this);
        });        
    }

    /*
     * Update the on-screen text displaying the number of queue walls.
     */
    public void setQueuedWalls(int numWalls)
    {
        Platform.runLater( ()-> {
            this.queuedWallsLabel.setText("Queued Walls: " + numWalls);
        });
    }
        

    /*
     * Update the on-screen text displaying the cooldown before the next wall can/will be placed
     */
    public void setWallCooldownText(long cooldownMillis)
    {
        if(cooldownMillis < 0)
        {
            throw new IllegalArgumentException("Can't set wall cooldown text for value " + cooldownMillis + " as it is less than 0");
        }


        if(cooldownMillis == 0l)
        {
            Platform.runLater( ()-> {
                this.wallCooldownLabel.setText("Wall Cooldown: READY");
            } );            
        }
        else
        {
            double cooldownSeconds = (double)cooldownMillis / 1000;

            Platform.runLater( ()-> {
                this.wallCooldownLabel.setText("Wall Cooldown: " + cooldownSeconds + "s");
            });            
        }
        

    }

    /*
     * Update the on-screen text displaying the number of walls that can still be placed.
     * Does not count queued walls.
     */
    public void setAvailableWallsText(int availableWalls)
    {
        Platform.runLater( ()-> {
            this.availableWallsLabel.setText("Available Walls: " + availableWalls);
        });        
    }

}
