package nsu.snake.peer;

import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.model.Model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Messages {
    public static DatagramPacket SendAnnouncementMsg(GameInfo curGameState, InetAddress group, int port, long msg_seq){
//        System.out.println("Send Announcement");
        SnakesProto.GameConfig conf = SnakesProto.GameConfig.newBuilder()
                .setFoodStatic(curGameState.config.getFood_static())
                .setHeight(curGameState.config.getHeight())
                .setWidth(curGameState.config.getWidth())
                .setStateDelayMs(curGameState.config.getState_delay_ms())
                .build();

        ArrayList<SnakesProto.GamePlayer> players = new ArrayList<>();
        setPlayersToProto(curGameState, players);
        SnakesProto.GamePlayers pl = SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build();

        SnakesProto.GameMessage.AnnouncementMsg ann = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                .addGames(SnakesProto.GameAnnouncement.newBuilder()
                        .setConfig(conf)
                        .setPlayers(pl)
                        .setGameName(curGameState.gameName))
                        .build();

        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();

        GameMsgBuilder.setAnnouncement(ann).setMsgSeq(msg_seq);
        SnakesProto.GameMessage gameMsg = GameMsgBuilder.build();

        return MakeDatagram(gameMsg, group, port);
    }

    public static String RecvAnnouncementMsg(GameInfo gameState, InetAddress sender_addr, int sender_port, SnakesProto.GameMessage msg){
        SnakesProto.GameAnnouncement ann = msg.getAnnouncement().getGames(0);
        SnakesProto.GameConfig conf = ann.getConfig();
        gameState.config = new GameInfo.GameConfig(conf.getWidth(), conf.getHeight(), conf.getFoodStatic(), conf.getStateDelayMs());

        List<SnakesProto.GamePlayer> players = ann.getPlayers().getPlayersList();
        ArrayList<GameInfo.GamePlayer> myPs = new ArrayList<>();
        setPlayersFromProto(players, myPs, sender_port, sender_addr);
        gameState.players = myPs;
        gameState.gameName = ann.getGameName();

        return ann.getGameName();
    }

    public static DatagramPacket SendSteerMsg(GameInfo.Direction direction,
                                    InetAddress master_ip,
                                    int master_port,
                                    long msg_seq){
        //NORMAL
        SnakesProto.Direction direction2 = null;
        switch (direction){
            case UP -> direction2 = SnakesProto.Direction.UP;
            case DOWN -> direction2 = SnakesProto.Direction.DOWN;
            case LEFT -> direction2 = SnakesProto.Direction.LEFT;
            case RIGHT -> direction2 = SnakesProto.Direction.RIGHT;
        }

        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.SteerMsg.Builder SteerMsgBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        SteerMsgBuilder.setDirection(direction2);
        GameMsgBuilder.setMsgSeq(msg_seq);
        GameMsgBuilder.setSteer(SteerMsgBuilder.build());
        SnakesProto.GameMessage gameMsg = GameMsgBuilder.build();

        return MakeDatagram(gameMsg, master_ip, master_port);
    }

    public static int[] RecvSteerMsg(Model model, SnakesProto.Direction direction, int sender_id){
        //MASTER
        return model.MoveSnake(sender_id, direction);
    }

    public static DatagramPacket SendAckMsg(long msg_seq, int sender_id, int receiver_id, InetAddress receiver_ip, int receiver_port){
        //NORMAL and MASTER каждый раз при подтверждении
        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.AckMsg AckMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        GameMsgBuilder.setMsgSeq(msg_seq);
        GameMsgBuilder.setAck(AckMsg);
        GameMsgBuilder.setSenderId(sender_id);
        GameMsgBuilder.setReceiverId(receiver_id);
        SnakesProto.GameMessage gameMsg = GameMsgBuilder.build();
        System.out.println("SEND Ack , msg_seq: " + msg_seq);

        return MakeDatagram(gameMsg, receiver_ip, receiver_port);
    }

    public static void RecvAckMsg(){
    }

    public static DatagramPacket SendStateMsg(long msg_seq, InetAddress receiver_ip, int receiver_port, GameInfo curState, int state_order){
        //MASTER
        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.StateMsg.Builder stateBuilder = SnakesProto.GameMessage.StateMsg.newBuilder();
        GameMsgBuilder.setMsgSeq(msg_seq);
        SnakesProto.GameState.Builder gameStateBuilder = SnakesProto.GameState.newBuilder();
        setSnakesToProto(curState, gameStateBuilder);
        setFoodToProto(curState, gameStateBuilder);
        gameStateBuilder.setStateOrder(state_order);

        ArrayList<SnakesProto.GamePlayer> players = new ArrayList<>();
        setPlayersToProto(curState, players);
        gameStateBuilder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build());

        stateBuilder.setState(gameStateBuilder.build());

        SnakesProto.GameMessage gameMsg = GameMsgBuilder.setState(stateBuilder.build()).build();

        System.out.println("SEND State , msg_seq: " + msg_seq);
        return MakeDatagram(gameMsg, receiver_ip, receiver_port);
    }

    private static DatagramPacket MakeDatagram(SnakesProto.GameMessage gameMsg, InetAddress receiver_ip, int receiver_port){
        byte[] serializedData = gameMsg.toByteArray();
        DatagramPacket datagram = new DatagramPacket(serializedData, serializedData.length);
        datagram.setAddress(receiver_ip);
        datagram.setPort(receiver_port);
        return datagram;
    }

    public static void SendGameMsg(DatagramPacket datagram, MulticastSocket socket){
        try{ socket.send(datagram); } catch (IOException ex) {ex.printStackTrace();}
    }

    private static void setSnakesToProto(GameInfo curState, SnakesProto.GameState.Builder gameStateBuilder){
        ArrayList<GameInfo.Snake> snakes = curState.snakes;
        for(int i = 0; i < snakes.size(); ++i){
            GameInfo.Snake myS = snakes.get(i);
            SnakesProto.Direction head = null;
            switch (myS.head_direction){
                case RIGHT -> head = SnakesProto.Direction.RIGHT;
                case LEFT -> head = SnakesProto.Direction.LEFT;
                case UP -> head = SnakesProto.Direction.UP;
                case DOWN -> head = SnakesProto.Direction.DOWN;
            }
            ArrayList<SnakesProto.GameState.Coord> points = new ArrayList<>();
            for(int j = 0; j < myS.points.size(); ++j){
                points.add(SnakesProto.GameState.Coord.newBuilder().setX(myS.points.get(j).getX()).setY(myS.points.get(j).getY()).build());
            }
            SnakesProto.GameState.Snake snake = SnakesProto.GameState.Snake.newBuilder()
                    .setState(SnakesProto.GameState.Snake.SnakeState.valueOf(myS.state))
                    .setHeadDirection(head)
                    .setPlayerId(myS.player_id)
                    .addAllPoints(points).build();
            gameStateBuilder.addSnakes(snake);
        }
    }

    private static void setFoodToProto(GameInfo curState, SnakesProto.GameState.Builder gameStateBuilder){
        ArrayList<GameInfo.Coord> food = curState.food;
        ArrayList<SnakesProto.GameState.Coord> foodnew = new ArrayList<>();
        for(int j = 0; j < food.size(); ++j){
            foodnew.add(SnakesProto.GameState.Coord.newBuilder().setX(food.get(j).getX()).setY(food.get(j).getY()).build());
        }
        gameStateBuilder.addAllFoods(foodnew);
    }

    private static void setPlayersToProto(GameInfo curState, ArrayList<SnakesProto.GamePlayer> players){
        for(int j = 0; j < curState.players.size(); ++j){
            GameInfo.GamePlayer myP = curState.players.get(j);
            SnakesProto.NodeRole newRole = null;
            switch (myP.getRole()){
                case MASTER -> newRole = SnakesProto.NodeRole.MASTER;
                case DEPUTY -> newRole = SnakesProto.NodeRole.DEPUTY;
                case NORMAL -> newRole = SnakesProto.NodeRole.NORMAL;
                case VIEWER -> newRole = SnakesProto.NodeRole.VIEWER;
            }

            SnakesProto.GamePlayer player = SnakesProto.GamePlayer.newBuilder()
                    .setName(myP.getName())
                            .setId(myP.getId())
                                    .setPort(myP.getPort())
                                            .setScore(myP.getScore())
                                                    .setIpAddress(myP.getIp_address().getHostName())
                                                            .setRole(newRole)
                                                                    .setType(SnakesProto.PlayerType.valueOf(myP.getType()))
                                                                            .build();
            players.add(player);
        }
    }

    public static void RecvStateMsg(GameInfo curGameState, SnakesProto.GameMessage deserializeMsg){
        //NORMAL
        SnakesProto.GameState stateMsg = deserializeMsg.getState().getState();
        ArrayList<GameInfo.Snake> mySs = new ArrayList<>();
        setSnakesFromProto(stateMsg, mySs);
        ArrayList<GameInfo.GamePlayer> myPs = new ArrayList<>();
        List<SnakesProto.GamePlayer> players = stateMsg.getPlayers().getPlayersList();
        setPlayersFromProto(players, myPs);
        ArrayList<GameInfo.Coord> myF = new ArrayList<>();
        setFoodFromProto(stateMsg, myF);

        curGameState.players = myPs;
        curGameState.snakes = mySs;
        curGameState.food = myF;
        curGameState.state_order = stateMsg.getStateOrder();
    }

    private static void setSnakesFromProto(SnakesProto.GameState stateMsg, ArrayList<GameInfo.Snake> mySs){
        List<SnakesProto.GameState.Snake> snakes = stateMsg.getSnakesList();
        for(int i = 0; i < snakes.size(); ++i){
            int state = snakes.get(i).getState().getNumber();
            int id = snakes.get(i).getPlayerId();
            GameInfo.Direction dir = null;
            switch (snakes.get(i).getHeadDirection()){
                case RIGHT -> dir = GameInfo.Direction.RIGHT;
                case LEFT -> dir = GameInfo.Direction.LEFT;
                case UP -> dir = GameInfo.Direction.UP;
                case DOWN -> dir = GameInfo.Direction.DOWN;
            }

            ArrayList<GameInfo.Coord> c = new ArrayList<>();
            List<SnakesProto.GameState.Coord> cc = snakes.get(i).getPointsList();
            for(int j = 0; j < cc.size(); ++j){
                c.add(new GameInfo.Coord(cc.get(j).getX(), cc.get(j).getY()));
            }
            mySs.add(new GameInfo.Snake(id, c, state, dir));
        }
    }

    private static void setPlayersFromProto(List<SnakesProto.GamePlayer> players, ArrayList<GameInfo.GamePlayer> myPs){
        for(int i = 0; i < players.size(); ++i){
            try {
            int id = players.get(i).getId();
            int port = players.get(i).getPort();
            int score = players.get(i).getScore();
            String name = players.get(i).getName();
            InetAddress addr = InetAddress.getByName(players.get(i).getIpAddress());
            GameInfo.NodeRole n = null;
            switch (players.get(i).getRole()){
                case MASTER -> n = GameInfo.NodeRole.MASTER;
                case DEPUTY -> n = GameInfo.NodeRole.DEPUTY;
                case NORMAL -> n = GameInfo.NodeRole.NORMAL;
                case VIEWER -> n = GameInfo.NodeRole.VIEWER;
            }

            GameInfo.GamePlayer p = new GameInfo.GamePlayer(name, id, addr, port, n, score);
            myPs.add(p);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void setPlayersFromProto(List<SnakesProto.GamePlayer> players, ArrayList<GameInfo.GamePlayer> myPs, int master_port, InetAddress master_addr){
        for(int i = 0; i < players.size(); ++i){
            try {
                int id = players.get(i).getId();
                int port = players.get(i).getPort();
                int score = players.get(i).getScore();
                String name = players.get(i).getName();
                InetAddress addr = InetAddress.getByName(players.get(i).getIpAddress());
                GameInfo.NodeRole n = null;
                switch (players.get(i).getRole()){
                    case MASTER -> n = GameInfo.NodeRole.MASTER;
                    case DEPUTY -> n = GameInfo.NodeRole.DEPUTY;
                    case NORMAL -> n = GameInfo.NodeRole.NORMAL;
                    case VIEWER -> n = GameInfo.NodeRole.VIEWER;
                }
                if(n == GameInfo.NodeRole.MASTER){
                    port = master_port;
                    addr = master_addr;
                }

                GameInfo.GamePlayer p = new GameInfo.GamePlayer(name, id, addr, port, n, score);
                myPs.add(p);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static void setFoodFromProto(SnakesProto.GameState stateMsg, ArrayList<GameInfo.Coord> myF){
        List<SnakesProto.GameState.Coord> food = stateMsg.getFoodsList();
        for(int i = 0; i < food.size(); ++i){
            myF.add(new GameInfo.Coord(food.get(i).getX(), food.get(i).getY()));
        }
    }

    public static DatagramPacket SendRoleChangeMsg(long msg_seq,
                                                   int senderRole, int receiverRole,
                                                   int senderId, int receiverId,
                                                   int receiver_port, InetAddress receiver_ip){
        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.RoleChangeMsg.Builder builder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
        GameMsgBuilder.setMsgSeq(msg_seq);
        builder.setReceiverRole(SnakesProto.NodeRole.forNumber(receiverRole));
        builder.setSenderRole(SnakesProto.NodeRole.forNumber(senderRole));
        GameMsgBuilder.setSenderId(senderId);
        GameMsgBuilder.setReceiverId(receiverId);

        SnakesProto.GameMessage gameMsg = GameMsgBuilder.setRoleChange(builder.build()).build();
        return MakeDatagram(gameMsg, receiver_ip, receiver_port);
    }

    public static void RecvRoleChangeMsg(){

    }

    public static DatagramPacket SendPingMsg(long msg_seq, int receiver_port, InetAddress receiver_ip){
        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.PingMsg PingMsg = SnakesProto.GameMessage.PingMsg.newBuilder().build();
        GameMsgBuilder.setMsgSeq(msg_seq);
        GameMsgBuilder.setPing(PingMsg);
        SnakesProto.GameMessage gameMsg = GameMsgBuilder.build();

        return MakeDatagram(gameMsg, receiver_ip, receiver_port);
    }

    public static void RecvPingMsg(){
    }

    public static void RecvErrorMsg(){
        //NORMAL
    }

    public static void SendErrorMsg(){
        //MASTER
        // TODO тут надо написать строку с сообщением об ошибке, чтобы потом ее отобразить
    }

    public static DatagramPacket SendJoinMsg(InetAddress masterIp, int masterPort,
                                      String playerName, String gameName,
                                      SnakesProto.NodeRole role){
        // NORMAL (только в начале)
        // Для присоединения к игре отправляется сообщение JoinMsg узлу, от которого было получено сообщение AnnouncementMsg.
        // В этом сообщении указывается имя игры, к которой хотим присоединиться (в текущей версии задачи это неважно, оставлено на будущее).
        // Также указывается режим присоединения, стандартный или "только просмотр", во втором случае игрок не получает змейку, а может только просматривать, что происходит на поле.

        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.JoinMsg join = SnakesProto.GameMessage.JoinMsg.newBuilder()
                        .setPlayerName(playerName)
                                .setGameName(gameName)
                                        .setRequestedRole(role).build();
        GameMsgBuilder.setMsgSeq(1);
        GameMsgBuilder.setJoin(join);
        SnakesProto.GameMessage gameMsg = GameMsgBuilder.build();

        return MakeDatagram(gameMsg, masterIp, masterPort);
    }

    public static boolean RecvJoinMsg(Model model, int player_id){
        //MASTER
//      Когда к игре присоединяется новый игрок, MASTER-узел находит на поле квадрат 5x5 клеток, в котором нет клеток, занятых змейками.
//      Квадрат ищется с учётом того, что края поля замкнуты.
//      Для нового игрока создаётся змейка длиной две клетки, её голова помещается в центр найденного квадрата,
//      а хвост - случайным образом в одну из четырёх соседних клеток.
//      На двух клетках, которые займёт новая змейка, не должно быть еды, иначе ищется другое расположение.
//      Исходное направление движения змейки противоположно выбранному направлению хвоста.
//      Число очков присоединившегося игрока равно 0.
//      Если не удалось найти подходящий квадрат 5x5, то пытающемуся присоединиться игроку отправляется ErrorMsg с сообщением об ошибке.
//      Если удалось разместить новую змейку на поле, то новому игроку присваивается уникальный идентификатор в пределах текущего состояния игры.
//      В ответ на JoinMsg отправляется сообщение AckMsg, в котором игроку сообщается его идентификатор в поле receiver_id.
        boolean canJoin = model.AddNewSnake(player_id);
        return canJoin;
    }
}
