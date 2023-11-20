package nsu.snake.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import nsu.snake.peer.Peer;
import nsu.snake.view.StartWindow;

public class GameWindowController {

    public GameWindowController(Stage parentStage, Peer peer){
        this.parentStage = parentStage;
        this.peer = peer;
    }
    @FXML
    private void ExitClicked(){
        StartWindow start = new StartWindow(peer);
        parentStage.close();
        start.start(new Stage());
    }

    private Stage parentStage;
    private Peer peer;
    @FXML
    private Button exitButton;
}
