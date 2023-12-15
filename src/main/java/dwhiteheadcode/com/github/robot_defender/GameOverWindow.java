package dwhiteheadcode.com.github.robot_defender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;

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
        Optional<Integer> previousHighScore = getPreviousHighScore();
        
        String highScoreText;

        if( isNewHighScore(previousHighScore, finalScore) )
        {
            int highScore = finalScore;
            highScoreText = "New High Score: " + highScore + "!";

            try
            {
                saveHighScore(highScore);
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

    /*
     * Gets the previously saved highscore.
     * 
     * Returns either:
     *      An empty Optional if no highscore could be loaded.
     * 
     *      OR
     * 
     *      An Optional containing the previously saved highscore.
     */
    private Optional<Integer> getPreviousHighScore()
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

    /*
     * Attempts to save the given score to the HighScore file, deleting the previously saved highscore (if there was one).
     */
    private void saveHighScore(int score) throws IOException
    {
        File highScoreFile = new File(Main.HIGHSCORE_FILE_NAME);

        if(highScoreFile.exists())
        {
            highScoreFile.delete();
        }

        OutputStreamWriter osw = new OutputStreamWriter( new FileOutputStream(highScoreFile));
        
        try(BufferedWriter bw = new BufferedWriter(osw))
        {
            String scoreString = String.valueOf(score);

            bw.write(scoreString);
        }        
    }
    
}
