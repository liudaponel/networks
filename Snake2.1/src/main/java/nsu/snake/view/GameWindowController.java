package nsu.snake.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import nsu.snake.peer.GameInfo;
import nsu.snake.peer.Messages;
import nsu.snake.peer.Peer;
import nsu.snake.peer.Server;
import nsu.snake.view.StartWindow;

import java.io.IOException;
import java.net.*;
import java.lang.Math;

public class GameWindowController {
    public GameWindowController(Stage parentStage, int width, int height, MulticastSocket socket, int my_id, GameInfo.NodeRole myRole, Stage swStage, Server myServer){
        this.parentStage = parentStage;

        int max = Integer.max(width, height);
        if(max <= 20){
            SIZE_SQUARE = 25;
        } else if(max <= 30){
            SIZE_SQUARE = 16;
        } else if(max <= 40){
            SIZE_SQUARE = 12;
        } else if(max <= 70){
            SIZE_SQUARE = 7;
        } else if(max <= 100){
            SIZE_SQUARE = 5;
        }
        PIX_WIDTH = SIZE_SQUARE * width;
        PIX_HEIGHT = SIZE_SQUARE * height;

        this.socket = socket;
        this.my_id = my_id;
        this.myRole = myRole;
        this.swStage = swStage;
        this.myServer = myServer;
    }
    @FXML
    private void ExitClicked(){
        parentStage.close();
        swStage.show();
        myServer.FinishGame();
        Platform.runLater(() -> {
            master.setText(null);
            eat.setText(null);
            size.setText(null);
            vboxTop.getChildren().clear();
        });
    }

    public void UpdateCanvas(GameInfo curState){
        this.curState = curState;

        for(int m = 0; m < curState.players.size(); ++m){
            if(curState.players.get(m).getRole() == GameInfo.NodeRole.MASTER){
                masterAddr = curState.players.get(m).getIp_address();
                masterPort = curState.players.get(m).getPort();
                masterName = curState.players.get(m).getName();
                if(my_id == curState.players.get(m).getId()){
                    try {
                        masterAddr = InetAddress.getByName("127.0.0.1");
                    } catch(UnknownHostException ex){
                        ex.printStackTrace();
                    }
                }
                break;
            }
        }

        GameInfo.GameConfig config = curState.config;
        GraphicsContext graphContext = canvas.getGraphicsContext2D();

        graphContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        //рисовать змеек на поле
        for(int i = 0; i < curState.snakes.size(); ++i){
            GameInfo.Snake snake = curState.snakes.get(i);
            int x = 0;
            int y = 0;
            Color color = Color.MEDIUMVIOLETRED;
            if(snake.player_id == my_id){
                color = Color.GOLD;
            }
            graphContext.setFill(color);
            graphContext.fillRect((((x + snake.points.get(0).getX()) % config.getWidth() + config.getWidth()) % config.getWidth()) * SIZE_SQUARE,
                    (((y + snake.points.get(0).getY()) % config.getHeight() + config.getHeight()) % config.getHeight()) * SIZE_SQUARE,
                    SIZE_SQUARE, SIZE_SQUARE);
            x = snake.points.get(0).getX();
            y = snake.points.get(0).getY();

            for(int j = 1; j < snake.points.size(); ++j){
                color = Color.FUCHSIA;
                if(snake.player_id == my_id){
                    color = Color.YELLOW;
                }
                graphContext.setFill(color);

                int curX = snake.points.get(j).getX();
                int curY = snake.points.get(j).getY();
                if(curX != 0) {
                    int sign = 1;
                    if(curX < 0){
                        sign = -1;
                    }
                    for (int k = 1; k <= Math.abs(curX); ++k) {
                        graphContext.fillRect((((x + sign * k) % config.getWidth() + config.getWidth()) % config.getWidth()) * SIZE_SQUARE,
                                ((y % config.getHeight() + config.getHeight()) % config.getHeight()) * SIZE_SQUARE,
                                SIZE_SQUARE, SIZE_SQUARE);
                    }
                }
                if(curY != 0) {
                    int sign = 1;
                    if(curY < 0){
                        sign = -1;
                    }
                    for (int k = 1; k <= Math.abs(curY); ++k) {
                       graphContext.fillRect(((x % config.getWidth() + config.getWidth()) % config.getWidth()) * SIZE_SQUARE,
                                (((y + sign * k) % config.getHeight() + config.getHeight()) % config.getHeight()) * SIZE_SQUARE,
                                SIZE_SQUARE, SIZE_SQUARE);
                    }
                }

                x += curX;
                y += curY;
            }
        }

        //еда
        for(int j = 0; j < curState.food.size(); ++j){
            graphContext.setFill(Color.RED);
            graphContext.fillRect(curState.food.get(j).getX() * SIZE_SQUARE,
                    curState.food.get(j).getY() * SIZE_SQUARE,
                    SIZE_SQUARE, SIZE_SQUARE);
        }

        //горизонтальные
        for(int i = 0; i <= config.getHeight(); ++i){
            graphContext.strokeLine(0, i * SIZE_SQUARE,
                    PIX_WIDTH, i * SIZE_SQUARE);
        }
        //вертикальные
        for(int i = 0; i <= config.getWidth(); ++i) {
            graphContext.strokeLine(i * SIZE_SQUARE, 0,
                    i * SIZE_SQUARE, PIX_HEIGHT);
        }
    }

    public void UpdateRecords(){
        Platform.runLater(() -> {
            master.setText(masterName);
            eat.setText(curState.config.getFood_static() + " + " + curState.players.size());
            size.setText(curState.config.getWidth() + " x " + curState.config.getHeight());

            vboxTop.getChildren().clear();
            for(int i = 0; i < curState.players.size(); ++i) {
                Label label = new Label(curState.players.get(i).getName() + "  " + curState.players.get(i).getScore());
                label.setStyle("-fx-font-size: 20px;");
                vboxTop.getChildren().add(label);
            }
        });
    }


    public void setKeysTab(GameInfo.Direction direction){
        myServer.SendSteerMsg(direction);
    }

    private Stage parentStage;
    private int SIZE_SQUARE = 0;
    private int PIX_WIDTH = 0;
    private int PIX_HEIGHT = 0;
    private MulticastSocket socket;
    private  volatile GameInfo curState;
    int msg_seq = 1;
    int my_id = 0;
    GameInfo.NodeRole myRole = null;
    InetAddress masterAddr = null;
    int masterPort = 0;
    String masterName = "";
    Stage swStage = null;
    Server myServer = null;
    @FXML
    private Button exitButton;
    @FXML
    private Canvas canvas;
    @FXML
    private Label master;
    @FXML
    private Label size;
    @FXML
    private Label eat;
    @FXML
    private VBox vboxTop;
}
