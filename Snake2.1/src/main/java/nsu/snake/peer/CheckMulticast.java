package nsu.snake.peer;

import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.peer.GameInfo;

import java.util.ArrayList;
import java.util.HashMap;

public class CheckMulticast implements Runnable{
    @Override
    public void run() {
        // создать поток для бесконечного массива, чтобы этот поток мог делать getCurGames
        Thread checker = new Thread(new CheckAnnouncementMsg());
        checker.start();
    }

    public HashMap<String, GameInfo> getCurGames(){
        return games;
    }

    private class CheckAnnouncementMsg implements Runnable{
        //  AnnouncementMsg announcement = 6;
        //  TODO сдесь просто бесконечно получается и обновляется информация. Есть какая-то инфа и я ее могу взять
        @Override
        public void run(){
            // TODO когда получаю сообщение, в GameInfo -> Players мастеру дописать его ip и port,
            // взятые из датаграммы, чтобы я потом в startWindow могла находить у мастера ip,port
        }
    }

    /**
     * По уникальному названию игры информация о ней
     */
    HashMap<String, GameInfo> games = new HashMap<>();
}
