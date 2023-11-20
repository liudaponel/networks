package nsu.snake.peer;

import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.peer.GameInfo;

import java.util.ArrayList;
import java.util.HashMap;

public class CheckMulticast implements Runnable{
    @Override
    public void run() {

    }

    public HashMap<String, GameInfo> getCurGames(){
        return games;
    }
    //        AnnouncementMsg announcement = 6;
    //        ErrorMsg error = 8;
    // TODO сдесь просто бесконечно получается и обновляется информация. Есть какая-то инфа и я ее могу взять
    /**
     * По уникальному названию игры информация о ней
     */
    HashMap<String, GameInfo> games = new HashMap<>();
}
