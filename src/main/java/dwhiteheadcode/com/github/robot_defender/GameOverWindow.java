package dwhiteheadcode.com.github.robot_defender;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GameOverWindow 
{
    public void display(int finalScore, GameWindow app)
    {
        Stage gameOverWindow = new Stage();
        gameOverWindow.initModality(Modality.WINDOW_MODAL);
        gameOverWindow.setTitle("Robot Defender - GAME OVER");
        gameOverWindow.setMinWidth(400);
        gameOverWindow.setMinHeight(100);

        Label finalScoreLabel = new Label("Final Score: " + finalScore);
        Button restartButton = new Button("Play Again");
        restartButton.setOnAction(
            e -> {
                app.startNewGame();
                gameOverWindow.close();
            }
        );

        VBox layout = new VBox(10);
        layout.getChildren().addAll(finalScoreLabel, restartButton);
        layout.setAlignment(Pos.CENTER);
        
        Scene scene = new Scene(layout);
        gameOverWindow.setScene(scene);
        gameOverWindow.showAndWait();
    }
}
