package nsu.snake;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import nsu.snake.view.GameWindow;
import nsu.snake.view.GameWindowController;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        HelloController controller = new HelloController(stage);
        fxmlLoader.setController(controller);

        Scene scene = null;
        try {
            Parent root = fxmlLoader.load();
            VBox vbox = (VBox) root;
            String style = "-fx-background-image: url('/nsu/snake/images/name2.jpg'); " +
                    "-fx-background-size: cover;" + "-fx-opacity: 0.9;";
            vbox.setStyle(style);

            scene = new Scene(vbox, 500, 300);
            stage.setTitle("Snake");
            stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("images/icon1.png")));
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}