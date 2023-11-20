package nsu.snake.peer;


import javafx.stage.Stage;
import nsu.snake.view.GameWindow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

public class Peer {
    public void start(String name) {
        if(name != null) {
            myName = name;
        }
        try {
            InetSocketAddress masterAddress = new InetSocketAddress(5555);
            MulticastSocket socketMult = new MulticastSocket(masterAddress);

            Thread checkMulticast = new Thread(checker);
            checkMulticast.start();

            Thread server = new Thread(myServer);
            server.start();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public void setNewGameConfig(GameInfo.GameConfig conf){
        gameConfig = conf;
        //вызывается этот метод только если я мастер в новой игре
        myServer.setIamMaster(conf);
    }

    public GameInfo getCurState(){
        return myServer.getCurState();
    }

    public HashMap<String, GameInfo> getGames(){
        return checker.getCurGames();
    }

    private String myName = "unknown";
    private Server myServer = new Server(myName);
    private CheckMulticast checker = new CheckMulticast();
    private GameInfo.GameConfig gameConfig;
}
