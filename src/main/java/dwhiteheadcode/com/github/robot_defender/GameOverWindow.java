package dwhiteheadcode.com.github.robot_defender;

import java.io.IOException;
import java.util.Optional;

import dwhiteheadcode.com.github.robot_defender.misc.HighScoreAccessor;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * The window that is shown when a GameOver occurs (a robot reaches the citadel).
 */
public class GameOverWindow 
{
    private Label errorText;

    /*
     * Displays a window showing the score at the time of the game over, the highscore,
     * and a button to restart the game. 
     * 
     * The GameWindow can't be interacted with until this window is closed. 
     */
    public void display(int finalScore, GameWindow app)
    {
        Optional<Integer> previousHighScore = new HighScoreAccessor().getHighScore();
        
        String highScoreText;

        if( isNewHighScore(previousHighScore, finalScore) )
        {
            int highScore = finalScore;
            highScoreText = "New High Score: " + highScore + "!";

            try
            {
                new HighScoreAccessor().saveHighScore(highScore);
            }
            catch(IOException ioE)
            {
                errorText = new Label("Error saving highscore: " + ioE.getMessage());
            }            
        }
        else
        {
            int highScore = previousHighScore.get();
            highScoreText = "High Score: " + highScore;
        }        
        
        Stage gameOverWindow = new Stage();
        gameOverWindow.initModality(Modality.WINDOW_MODAL);
        gameOverWindow.setTitle("Robot Defender - GAME OVER");
        gameOverWindow.setMinWidth(400);
        gameOverWindow.setMinHeight(150);

        // Close the whole App if the GameOverWindow is dismissed
        gameOverWindow.setOnCloseRequest( (e)-> {
            Platform.exit();
        });

        Label finalScoreLabel = new Label("Final Score: " + finalScore);
        Label highScoreLabel = new Label(highScoreText);

        Button restartButton = new Button("Play Again");
        restartButton.setOnAction(
            e -> {
                app.startNewGame();
                gameOverWindow.close();
            }
        );

        VBox layout = new VBox(10);
        layout.getChildren().addAll(finalScoreLabel, highScoreLabel, restartButton);
        layout.setAlignment(Pos.CENTER);

        if(errorText != null)
        {
            layout.getChildren().add(errorText);
        }
        
        Scene scene = new Scene(layout);
        gameOverWindow.setScene(scene);
        gameOverWindow.showAndWait();
    }

    /*
     * Returns false if previousHighScore contains a value that is greater than newScore.
     * Returns true otherwise.
     */
    private boolean isNewHighScore(Optional<Integer> previousHighScore, int newScore)
    {
        if(previousHighScore.isEmpty() || newScore > previousHighScore.get())
        {
            return true;
        }
        
        return false;
    }

    

    
    
}
