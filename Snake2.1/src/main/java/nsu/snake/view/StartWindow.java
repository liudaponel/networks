package nsu.snake.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

        GridPane gridPane = controller.getGridPane();
        // TODO вот я изначально вывела и потом надо обновлять эту информацию как-то
        // TODO для обновления у меня будет мапа с текущими играми и при нажатии на строку я возьму i-й элемент мапы для JOIN
        // Обновлять будет контроллер раз в какое-то время по ф-ии UpdateGridPane, там и будет отсылаться запрос на JOIN
        // и запросы getGames в checkMulticast (через peer)
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);
        controller.AddRow("Вася", 3, 30, 40, 5);

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
