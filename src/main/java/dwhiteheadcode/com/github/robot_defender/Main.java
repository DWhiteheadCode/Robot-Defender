package dwhiteheadcode.com.github.robot_defender;

import java.util.Optional;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*
 * The entrypoint for the program. 
 */
public class Main extends Application
{
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
        Optional<Integer> highScore = new HighScoreAccessor().getHighScore();
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
    
    // Stops the GameWindow when the application is closed.
    @Override
    public void stop()
    {
        if(this.game != null)
        {
            this.game.stop();
        }        
    }

}
