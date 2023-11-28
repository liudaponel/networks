package nsu.snake.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nsu.snake.peer.Peer;

import java.io.IOException;

public class StartWindow extends Application {
    public StartWindow(Peer peer){
        this.peer = peer;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        FXMLLoader fxmlLoader = new FXMLLoader(StartWindow.class.getResource("startWindow-view.fxml"));
        StartWindowController controller = new StartWindowController(stage, peer);
        fxmlLoader.setController(controller);

        Scene scene = null;
        Parent root = null;
        try {
            root = fxmlLoader.load();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        stage.setTitle("Snake");
        stage.getIcons().add(new Image(GameWindow.class.getResourceAsStream("/nsu/snake/images/icon1.png")));

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> controller.UpdateGridPane()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        scene = new Scene(root, 600, 500);
        stage.setScene(scene);
        stage.show();
    }

    public void setName(String name){
        this.name = name;
    }
    private String name;
    private Peer peer;
}
