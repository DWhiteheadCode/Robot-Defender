package dwhiteheadcode.com.github.robot_defender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application
{
    public static final String HIGHSCORE_FILE_NAME = "HighScore.txt";
    private GameWindow game;

    public static void main(String[] args) 
    {
        launch(args);        
    }

    @Override
    public void start(Stage stage) throws Exception 
    {
        stage.setTitle("Robot Defender");

        Button startButton = new Button("Start");
        startButton.setOnAction(
            e -> {
                this.game = new GameWindow();
                this.game.start(stage);
            }
        );

        VBox layout = new VBox(10);

        // Show the highscore, if there is one
        Optional<Integer> highScore = getHighscore();
        if(highScore.isPresent())
        {
            Label highScoreLabel = new Label("High Score: " + highScore.get());
            layout.getChildren().add(highScoreLabel);
        }

        // Add the start button to the layout
        layout.getChildren().add(startButton);
        layout.setAlignment(Pos.CENTER);

        Scene mainMenuScene = new Scene(layout, 400, 100);
        stage.setScene(mainMenuScene);
        stage.show();
    }
    
    @Override
    public void stop()
    {
        if(this.game != null)
        {
            this.game.stop();
        }        
    }

    private Optional<Integer> getHighscore()
    {
        File highScoreFile = new File(Main.HIGHSCORE_FILE_NAME);

        if( ! highScoreFile.exists() )
        {
            return Optional.empty();
        }

        try(
            FileReader scoreReader = new FileReader(highScoreFile);
            BufferedReader buffer = new BufferedReader(scoreReader);)
        {
            String line = buffer.readLine();
            int highScore = Integer.valueOf(line);
            
            return Optional.of(highScore);
        }
        catch(IOException | NumberFormatException e )
        {
            return Optional.empty();
        }
    }



}
