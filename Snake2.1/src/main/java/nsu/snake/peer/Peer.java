package nsu.snake.peer;


import javafx.stage.Stage;
import nsu.snake.view.GameWindow;
import nsu.snake.view.GameWindowController;
import nsu.snake.view.StartWindow;
import nsu.snake.view.StartWindowController;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashMap;

public class Peer {
    public void start(String name) {
        if(name != null) {
//            try {
//                BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/nsu/snake/conf/ports.txt"));
//                String text = null;
//                int i = 0;
//                while ((text = reader.readLine()) != null) {
//                    ports[i] = Integer.parseInt(text);
//                    ++i;
//                }
//            } catch(IOException ex){ ex.printStackTrace(); }

            myName = name;
            myServer = new Server(myName);
            checker = new CheckMulticast();
            Thread checkMulticast = new Thread(checker);
            checkMulticast.start();
        }
    }

    public void setIamMaster(GameInfo.GameConfig conf){
        myServer.setIamMaster(conf);
    }

    public HashMap<String, GameInfo> getGames(){
        return checker.getCurGames();
    }

    public void setGameConfig(GameInfo.GameConfig conf){
        myServer.config = conf;
        myServer.curGameState.config = conf;
        myServer.setNormal();
    }
    public void StartGame(){
        Stage gwStage = new Stage();
        GameInfo state = myServer.getCurState();
        GameWindowController gwController = new GameWindowController(gwStage,
                                                state.config.getWidth(), state.config.getHeight(),
                                                myServer.getSocket(), myServer.getMyID(), myServer.getMyRole(), swStage, myServer);
        gameWindow = new GameWindow(gwController);
        gameWindow.start(gwStage);
        myServer.setGwController(gwController);
    }

    public void setSWStage(Stage st){
        swStage = st;
    }

    public String getMyName(){
        return myName;
    }

    public MulticastSocket getSocket(){
        return myServer.getSocket();
    }

    public boolean HasJoinOrError(){
        return myServer.HasJoinOrError();
    }

    private String myName = "unknown";
    private Server myServer;
    private CheckMulticast checker;
    private GameWindow gameWindow;
    private Stage swStage;
}
