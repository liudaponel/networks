package nsu.snake.view;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import nsu.snake.peer.Peer;

import java.util.Random;

import java.io.IOException;

public class GameWindow extends Application {
    public GameWindow(Peer peer){
        this.peer = peer;
    }
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(GameWindow.class.getResource("gameWindow-view.fxml"));
            GameWindowController controller = new GameWindowController(stage, peer);
            fxmlLoader.setController(controller);

            Parent root = fxmlLoader.load();
            BorderPane borderPane = (BorderPane) root;
            AddBackground(borderPane);



            Scene scene = new Scene(borderPane, 1000, 600);
            stage.setTitle("Snake");
            stage.getIcons().add(new Image(GameWindow.class.getResourceAsStream("/nsu/snake/images/icon1.png")));
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private void AddBackground(BorderPane borderPane){
        String resourceUrl[] = {
                GameWindow.class.getResource("/nsu/snake/images/wall2.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall4.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall5.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall6.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall7.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall8.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall9.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall10.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall11.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall12.jpg").toExternalForm(),
                GameWindow.class.getResource("/nsu/snake/images/wall13.jpg").toExternalForm()
        };
        Random random = new Random();
        int randomNumber = random.nextInt(11);
        String style = String.format("-fx-background-image: url('%s'); " +
                "-fx-background-size: cover;" + "-fx-opacity: 0.8;", resourceUrl[randomNumber]);
        borderPane.setStyle(style);
    }

    private Peer peer;
    @FXML
    private Canvas canvas;
}

