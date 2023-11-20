package nsu.snake.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import nsu.snake.peer.Messages;
import nsu.snake.peer.Peer;
import org.controlsfx.control.action.Action;

public class StartWindowController {
    public StartWindowController(Stage parentStage, Peer peer){
        this.parentStage = parentStage;
        this.peer = peer;
    }

    @FXML
    protected void EnterClicked(Button button){
        Integer rowIndex = GridPane.getRowIndex(button);
        // TODO get элемент i-й строки и добавить мапу с играми в контроллер

        boolean joinedSuccessful = Messages.SendJoinMsg();
        if(joinedSuccessful) {
            GameWindow game = new GameWindow(peer);
            parentStage.close();
            game.start(new Stage());
        }
    }

    @FXML
    protected void NewGameClicked(){
        parentStage.close();
        SetGameConfig conf = new SetGameConfig(peer);
        conf.start(new Stage());
    }

    public GridPane getGridPane() {
        return gridPane;
    }

    public void AddRow(String NameMaster, int countPlayers, int width, int height, int food) {
        Label nameLabel = new Label(NameMaster);
        nameLabel.setPrefWidth(250);
        Label playersLabel = new Label(Integer.toString(countPlayers));
        playersLabel.setPrefWidth(100);
        Label sizeLabel = new Label(Integer.toString(width) + " x " + Integer.toString(height));
        sizeLabel.setPrefWidth(100);
        Label foodLabel = new Label(Integer.toString(food));
        foodLabel.setPrefWidth(100);

        Button myButton = new Button();
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/nsu/snake/images/vyhod1.png")));
        myButton.setGraphic(imageView);
        myButton.setPrefSize(50, 30);
        myButton.setOnAction(event -> EnterClicked(myButton));

        int rowIndex = gridPane.getRowCount();
        gridPane.addRow(rowIndex, nameLabel, playersLabel, sizeLabel, foodLabel, myButton);

    }

    private Stage parentStage;
    private Peer peer;
    @FXML
    private GridPane gridPane;
    @FXML
    private Button newGameButton;
}
