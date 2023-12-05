package nsu.snake.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import nsu.snake.peer.GameInfo;
import nsu.snake.peer.Peer;

public class SetGameConfigController {
    SetGameConfigController(Stage parentStage, Peer peer){
        this.parentStage = parentStage;
        this.peer = peer;
    }

    @FXML
    protected void StartClicked(){
        String width = widthField.getText();
        String height = heightField.getText();
        String foodStr = food.getText();
        String timeStr = time_delay.getText();
        GameInfo.GameConfig conf = new GameInfo.GameConfig(Integer.parseInt(width), Integer.parseInt(height), Integer.parseInt(foodStr), Integer.parseInt(timeStr));

        peer.setIamMaster(conf);

        parentStage.close();
        peer.StartGame();
    }

    private Peer peer;
    private Stage parentStage;
    @FXML
    private TextField widthField;
    @FXML
    private TextField heightField;
    @FXML
    private TextField food;
    @FXML
    private TextField time_delay;
    @FXML
    private Button startButton;
}
