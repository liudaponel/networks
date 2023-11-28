package nsu.snake;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import nsu.snake.peer.Peer;
import nsu.snake.view.StartWindow;

public class HelloController {
    public HelloController(Stage parentStage){
        this.parentStage = parentStage;
    }
    @FXML
    protected void StartButtonClicked() {
        String peerName = name.getText();
        Peer peer = new Peer();
        peer.start(peerName);

        Stage stage = new Stage();
        peer.setSWStage(stage);
        StartWindow startWindow = new StartWindow(peer);
        startWindow.start(stage);
        parentStage.close();
    }

    private Stage parentStage;
    @FXML
    private Button startButton;
    @FXML
    private TextField name;
}
