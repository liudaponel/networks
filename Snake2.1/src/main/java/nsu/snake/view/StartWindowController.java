package nsu.snake.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import nsu.snake.peer.GameInfo;
import nsu.snake.peer.Messages;
import nsu.snake.peer.Peer;
import org.controlsfx.control.action.Action;

import java.net.InetAddress;
import java.util.HashMap;

public class StartWindowController {
    public StartWindowController(Stage parentStage, Peer peer){
        this.parentStage = parentStage;
        this.peer = peer;
    }

    @FXML
    protected void EnterClicked(Button button){
        int i = GridPane.getRowIndex(button);
        InetAddress masterIp = null;
        int masterPort = 0;
        for(int m = 0; m < gamesArr[i].players.size(); ++m){
            if(gamesArr[i].players.get(m).getRole() == GameInfo.NodeRole.MASTER){
                masterIp = gamesArr[i].players.get(m).getIp_address();
                masterPort = gamesArr[i].players.get(m).getPort();
                break;
            }
        }
        peer.setGameConfig(gamesArr[i].config);

        boolean joinedSuccessful = Messages.SendJoinMsg(masterIp, masterPort);
        if(joinedSuccessful) {
            parentStage.hide();
            peer.StartGame();
        }
    }

    @FXML
    protected void NewGameClicked(){
        parentStage.hide();
        SetGameConfig conf = new SetGameConfig(peer);
        conf.start(new Stage());
    }

    public GridPane getGridPane() {
        return gridPane;
    }

    public void UpdateGridPane(){
        HashMap<String, GameInfo> games = peer.getGames();
        gamesArr = new GameInfo[games.size()];
        games.values().toArray(gamesArr);
        for(int i = 0; i < games.size(); ++i){
            String master = null;
            for(int m = 0; m < gamesArr[i].players.size(); ++m){
                if(gamesArr[i].players.get(m).getRole() == GameInfo.NodeRole.MASTER){
                    master = gamesArr[i].players.get(m).getName();
                    break;
                }
            }
            AddRow(master, gamesArr[i].players.size(), gamesArr[i].config.getWidth(), gamesArr[i].config.getHeight(), gamesArr[i].config.getFood_static());
        }
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
    @FXML
    private GridPane gridPane;
    @FXML
    private Button newGameButton;
    GameInfo[] gamesArr;
    Peer peer;
}
