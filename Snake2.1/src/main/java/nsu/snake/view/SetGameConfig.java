package nsu.snake.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import nsu.snake.HelloApplication;
import nsu.snake.HelloController;
import nsu.snake.peer.Peer;

import java.io.IOException;

public class SetGameConfig extends Application {
    public SetGameConfig(Peer peer){
        this.peer = peer;
    }
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        FXMLLoader fxmlLoader = new FXMLLoader(SetGameConfig.class.getResource("setGameConfig-view.fxml"));
        SetGameConfigController controller = new SetGameConfigController(stage, peer);
        fxmlLoader.setController(controller);

        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), 500, 300);
            stage.setTitle("Snake");
            stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("images/icon1.png")));
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private Peer peer;
}
