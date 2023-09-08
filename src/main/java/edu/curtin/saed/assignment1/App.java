package edu.curtin.saed.assignment1;

import edu.curtin.saed.assignment1.game_engine.GameEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application 
{
    private JFXArena arena;
    private TextArea logger;
    private Label scoreLabel;
    private Label queuedWallsLabel;
    private GameEngine gameEngine;

    public static void main(String[] args) 
    {
        launch();        
    }
    
    @Override
    public void start(Stage stage) 
    {
        stage.setTitle("20232430 - Assignment 1 Submission");       
        
        int numRows = 9;
        int numCols = 9;

        this.gameEngine = new GameEngine(this, numRows, numCols);

        this.arena = new JFXArena(gameEngine, numRows, numCols);
        gameEngine.setArena(arena);
        arena.addListener(gameEngine);

        gameEngine.start();

        arena.addListener((x, y) ->
        {
            System.out.println("Arena click at (" + x + "," + y + ")");
        });
        
        ToolBar toolbar = new ToolBar();
        scoreLabel = new Label("Score: 0");
        queuedWallsLabel = new Label("Queued Walls: 0");
        toolbar.getItems().addAll(scoreLabel, queuedWallsLabel);
                    
        this.logger = new TextArea();
        
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(arena, logger);
        arena.setMinWidth(300.0);
        
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);
        
        Scene scene = new Scene(contentPane, 800, 800);
        stage.setScene(scene);

        stage.show();
    }

    public JFXArena getArena()
    {
        return this.arena;
    }

    public void log(String message)
    {
        logger.appendText(message);
    }

    public void setScore(int score)
    {
        this.scoreLabel.setText("Score: " + score);
    }

    public void gameOver(int finalScore)
    {
        this.scoreLabel.setText("GAME OVER! Final Score: " + finalScore);
        gameEngine.stop();
    }

    public void setQueuedWalls(int numWalls)
    {
        this.queuedWallsLabel.setText("Queued Walls: " + numWalls);
    }

}
