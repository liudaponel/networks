package nsu.snake.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import nsu.snake.peer.GameInfo;
import nsu.snake.peer.Peer;

import java.util.Random;
import java.io.IOException;

public class GameWindow extends Application {
    public GameWindow(GameWindowController controller){
        this.controller = controller;
    }
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(GameWindow.class.getResource("gameWindow-view.fxml"));
            fxmlLoader.setController(controller);

            Parent root = fxmlLoader.load();
            BorderPane borderPane = (BorderPane) root;
            //Canvas canvas = (Canvas)borderPane.getCenter();
            AddBackground(borderPane);

            // TODO во всех классах сделать завершение потоков по нажатии на крестик окна

//            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(peer.getCurState().config.getState_delay_ms()), event -> controller.UpdateCanvas()));
//            timeline.setCycleCount(Timeline.INDEFINITE);
//            timeline.play();

            Scene scene = new Scene(borderPane, 1000, 600);
            stage.setTitle("Snake");
            stage.getIcons().add(new Image(GameWindow.class.getResourceAsStream("/nsu/snake/images/icon1.png")));
            stage.setScene(scene);
            stage.show();

            setKeys(scene, controller);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private void setKeys(Scene scene, GameWindowController controller){
        scene.setOnKeyPressed((KeyEvent event) -> {
            if (event.getText().isEmpty())
                return;
            char keyEntered = event.getText().toUpperCase().charAt(0);
            if (keyEntered == 'A' || keyEntered == 'Ф'){
                controller.setKeysTab(GameInfo.Direction.LEFT);
            }
            else if(keyEntered == 'W' || keyEntered == 'Ц'){
                controller.setKeysTab(GameInfo.Direction.UP);
            }
            else if(keyEntered == 'D' || keyEntered == 'В'){
                controller.setKeysTab(GameInfo.Direction.RIGHT);
            }
            else if(keyEntered == 'S' || keyEntered == 'Ы'){
                controller.setKeysTab(GameInfo.Direction.DOWN);
            }
        });
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

    GameWindowController controller = null;
}

