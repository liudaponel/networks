package nsu.snake.peer;

import nsu.snake.model.Model;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.view.GameWindowController;

public class Server {
    Server(String name){
        myName = name;
        Thread reader = new Thread(new Reader());
        reader.start();
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            long msg_seq_last = 0; //номер сообщения
            long msg_seq_new = 0;
            int sender_id = 0; //(обязательно для AckMsg и RoleChangeMsg)
            int receiver_id = 0;

            InetAddress sender_addr = null;
            int sender_port = 0;
            try {
                socket = new MulticastSocket();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            while (true) {
                byte[] buff = new byte[1000];
                DatagramPacket datagram = new DatagramPacket(buff, buff.length);
                try {
                    socket.receive(datagram);
                    sender_addr = datagram.getAddress();
                    sender_port = datagram.getPort();
                    System.out.println("I receive smth from  " + sender_addr + " " + sender_port);

                    byte[] bytes = new byte[datagram.getLength()];
                    System.arraycopy(datagram.getData(), 0, bytes, 0, datagram.getLength());
                    SnakesProto.GameMessage deserializeMsg = SnakesProto.GameMessage.parseFrom(bytes);


                    sender_id = deserializeMsg.getSenderId();
                    msg_seq_new = deserializeMsg.getMsgSeq();

                    if (deserializeMsg.hasPing()) {
                        Messages.RecvPingMsg();
                    }
                    else if (deserializeMsg.hasAck()) {
                        Messages.RecvAckMsg();
                        if (msg_seq_last == 0) {
                            receiver_id = deserializeMsg.getReceiverId();
                            my_id = receiver_id;
                            myPort = socket.getLocalPort();
                        }
                        master_ip = sender_addr;
                        master_port = sender_port;
                    }
                    else if (deserializeMsg.hasError()) {
                        Messages.RecvErrorMsg();
                    }
                    else if (deserializeMsg.hasRoleChange()) {
                        //получается если я стал мастером, надо включить MasterWork и поменять у себя инфу
                        Messages.RecvRoleChangeMsg();
                    }
                    else if (deserializeMsg.hasState()) {
                        //Messages.RecvStateMsg(curGameState, deserializeMsg);
                    }
                    else if (deserializeMsg.hasJoin()) {
                        if(role == GameInfo.NodeRole.MASTER) {
                            boolean canJoin = Messages.RecvJoinMsg(model, curGameState.players.size() + 1);
                            if(canJoin){
                                String name = deserializeMsg.getJoin().getPlayerName();
                                int id = AddNewPlayer(name, sender_addr, sender_port);
                                curGameState.snakes = model.getSnakes();
                                Messages.SendAckMsg(msg_seq_last, my_id, sender_addr, sender_port, socket, id);
                            }
                            else{
                                Messages.SendErrorMsg();
                            }
                        }
                    }
                    else if (deserializeMsg.hasSteer()){
                        if(role == GameInfo.NodeRole.MASTER) {
                            if(sender_addr.isLoopbackAddress()){
                                sender_id = my_id;
                            }
                            else {
                                GameInfo cur = curGameState;
                                for (int j = 0; j < cur.players.size(); ++j) {
                                    if (cur.players.get(j).getIp_address() == sender_addr && cur.players.get(j).getPort() == sender_port){
                                        sender_id = cur.players.get(j).getId();
                                    }
                                }
                            }

                            SnakesProto.Direction direction = deserializeMsg.getSteer().getDirection();
                            if(!msg_seq_lastForPlayers.containsKey(sender_id) || msg_seq_new > msg_seq_lastForPlayers.get(sender_id)){
                                System.out.println("update");
                                steersFromPlayers.put(sender_id, direction);
                                msg_seq_lastForPlayers.put(sender_id, msg_seq_new);
                            }
                        }
                    }
                    msg_seq_last = msg_seq_new;

                    // TODO добавить для отслеживания кто умер в массив sender_id время текущее о приеме сообщения
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    class SenderAnnounce implements Runnable {
        @Override
        public void run(){
            //тут будет в бесконечном цикле отправляться Announcement на мультикаст
            //TODO этот цикл ЗАКАНЧИВАЕТСЯ, когда игра заканчивается!!!! while(gameIsPlaying) AtomicInt gameIsPlaying
            //а еще надо это вызывать из DEPUTY
            while(true){

            }
        }
    }
    class SenderGameState implements Runnable {
        @Override
        public void run(){
            //gameState тут же
            //TODO этот цикл ЗАКАНЧИВАЕТСЯ, когда игра заканчивается!!!! while(gameIsPlaying) AtomicInt gameIsPlaying
            //а еще надо вызывать из DEPUTY этот поток
            while(true){
                if(role == GameInfo.NodeRole.MASTER) {
                    UpdateSteerMsgs();
                    for(int i = 0; i < curGameState.players.size(); ++i){
                        GameInfo.GamePlayer pl = curGameState.players.get(i);
                        if(pl.getId() != my_id){
                            Messages.SendStateMsg(msg_seq, pl.getIp_address(), pl.getPort(), socket, curGameState);
                        }
                    }
                }
                UpdateInfo();
                try{
                    Thread.sleep(curGameState.config.getState_delay_ms());
                } catch (InterruptedException ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    private void UpdateInfo(){
        if(gwController != null) {
            curGameState.food = model.SpawnEat(curGameState.players.size());
            gwController.UpdateCanvas(curGameState);
        }
    }

    private void UpdateSteerMsgs(){
        for(int i = 0; i < curGameState.players.size(); ++i) {
            int sender_id = curGameState.players.get(i).getId();
            SnakesProto.Direction direction = null;
            if(steersFromPlayers.containsKey(sender_id)){
                direction = steersFromPlayers.get(sender_id);
            } else{
                for(int j = 0; j < curGameState.snakes.size(); ++j){
                    if(curGameState.snakes.get(j).player_id == sender_id){
                        GameInfo.Direction dir = curGameState.snakes.get(j).head_direction;
                        switch (dir){
                            case UP -> direction = SnakesProto.Direction.UP;
                            case DOWN -> direction = SnakesProto.Direction.DOWN;
                            case LEFT -> direction = SnakesProto.Direction.LEFT;
                            case RIGHT -> direction = SnakesProto.Direction.RIGHT;
                        }
                    }
                }
            }

            if(direction != null) {
                int other_player_id = Messages.RecvSteerMsg(model, direction, sender_id);
                curGameState.snakes = model.getSnakes();

                if (other_player_id != 0) {
                    for (int j = 0; j < curGameState.players.size(); ++j) {
                        if (other_player_id == curGameState.players.get(j).getId()) {
                            curGameState.players.get(j).setScore(curGameState.players.get(j).getScore() + 1);
                        }
                    }
                }
            }
        }
        steersFromPlayers.clear();
        msg_seq_lastForPlayers.clear();
    }

    public void setIamMaster(GameInfo.GameConfig conf){
        try{
            byte[] buf = new String("Hello").getBytes();
            DatagramPacket d = new DatagramPacket(buf, buf.length);
            d.setAddress(InetAddress.getByName("127.0.0.1"));
            d.setPort(5555);
            socket.send(d);
        }
        catch (IOException ex){ex.printStackTrace();}
        myPort = socket.getLocalPort();

        role = GameInfo.NodeRole.MASTER;
        model = new Model(conf);
        curGameState = new GameInfo();
        try {
            AddNewPlayer(myName, InetAddress.getByName("127.0.0.1"), myPort);
            master_ip = InetAddress.getByName("127.0.0.1");
            master_port = myPort;
            model.AddNewSnake(1);
        } catch(UnknownHostException ex){ex.printStackTrace();}
        curGameState.config = conf;
        curGameState.snakes = model.getSnakes();

        Thread senderAnn = new Thread(new SenderAnnounce());
        senderAnn.start();
        Thread senderGS = new Thread(new SenderGameState());
        senderGS.start();
    }

    public GameInfo getCurState(){
        return curGameState;
    }

    private int AddNewPlayer(String name, InetAddress ip_address, int port){
        GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
        if(curGameState.players.size() == 0){
            role = GameInfo.NodeRole.MASTER;
        }
        if(curGameState.players.size() == 1){
            role = GameInfo.NodeRole.DEPUTY;
        }
        int id = curGameState.players.size() + 1;
        GameInfo.GamePlayer newPlayer = new GameInfo.GamePlayer(name, id, ip_address, port, role, 0);
        curGameState.players.add(newPlayer);
        return id;
    }

    public int getMyID(){
        return my_id;
    }
    public MulticastSocket getSocket(){
        return socket;
    }
    public GameInfo.NodeRole getMyRole(){
        return role;
    }

    public void setGwController(GameWindowController controller){
        gwController = controller;
    }

    private Model model = null;
    private GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
    public GameInfo curGameState = null;
    private String myName;
    private int my_id = 1;
    private int myPort = 0;
    public InetAddress master_ip = null;
    public int master_port = 0;
    private MulticastSocket socket = null;
    private GameWindowController gwController = null;
    private HashMap<Integer, SnakesProto.Direction> steersFromPlayers = new HashMap<>();
    private HashMap<Integer, Long> msg_seq_lastForPlayers = new HashMap<>();
    private long msg_seq = 0;
    // TODO HashMap по msg_seq сообщений, на которые не пришел Ack.
    // TODO HashMap по player.id для ping, если чел отвалился
}
