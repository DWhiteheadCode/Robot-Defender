package dwhiteheadcode.com.github.robot_defender;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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
        layout.getChildren().add(startButton);
        layout.setAlignment(Pos.CENTER);
        
        Scene mainMenuScene = new Scene(layout, 400, 100);
        stage.setScene(mainMenuScene);
        stage.show();
    }
    
    @Override
    public void stop()
    {
        this.game.stop();
    }

}
