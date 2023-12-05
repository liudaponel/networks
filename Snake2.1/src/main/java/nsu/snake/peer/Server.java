package nsu.snake.peer;

import javafx.scene.chart.PieChart;
import nsu.snake.model.Model;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.view.GameWindowController;

import javax.crypto.spec.PSource;

public class Server {
    Server(String name){
        myName = name;
        curGameState = new GameInfo();
        canJoin.set(-1);
        try {
            group = InetAddress.getByName("239.192.0.4");
            socket = new MulticastSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread reader = new Thread(new Reader());
        reader.start();
        Thread senderAnn = new Thread(new SenderAnnounce());
        senderAnn.start();
        Thread senderGS = new Thread(new SenderGameState());
        senderGS.start();
        Thread checkTime = new Thread(new CheckerTime());
        checkTime.start();
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            long msg_seq_new = 0;
            int sender_id = 0; //(обязательно для AckMsg и RoleChangeMsg)
            int receiver_id = 0;

            InetAddress sender_addr = null;
            int sender_port = 0;

            while (true) {
                byte[] buff = new byte[4096];
                DatagramPacket datagram = new DatagramPacket(buff, buff.length);
                try {
                    socket.receive(datagram);
                    sender_addr = datagram.getAddress();
                    sender_port = datagram.getPort();
//                    System.out.println("I receive smth from  " + sender_addr + " " + sender_port);

                    if (role != GameInfo.NodeRole.MASTER && sender_addr != master_ip && msg_seq_last != -1) {
                        continue;
                    }

                    byte[] bytes = new byte[datagram.getLength()];
                    System.arraycopy(datagram.getData(), 0, bytes, 0, datagram.getLength());
                    SnakesProto.GameMessage deserializeMsg = SnakesProto.GameMessage.parseFrom(bytes);

                    sender_id = deserializeMsg.getSenderId();
                    msg_seq_new = deserializeMsg.getMsgSeq();

                    if (deserializeMsg.hasPing()) {
                        System.out.println("Ping");
                        Messages.RecvPingMsg();
                    } else if (deserializeMsg.hasAck()) {
                        System.out.println("Ack");
                        Messages.RecvAckMsg();
                        if (msg_seq_last == -1) {
                            System.out.println("I received first ack");
                            receiver_id = deserializeMsg.getReceiverId();
                            my_id = receiver_id;
                            myPort = socket.getLocalPort();
                            canJoin.set(1);
                        }
                        master_ip = sender_addr;
                        master_port = sender_port;
                    } else if (deserializeMsg.hasError()) {
                        System.out.println("Error");
                        msg_seq_last = -1;
                        canJoin.set(0);
                        Messages.RecvErrorMsg();
                    } else if (deserializeMsg.hasRoleChange()) {
                        System.out.println("Role Change");
                        //получается если я стал мастером, надо включить MasterWork и поменять у себя инфу
                        Messages.RecvRoleChangeMsg();
                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                    } else if (deserializeMsg.hasState()) {
                        Messages.RecvStateMsg(curGameState, deserializeMsg);
                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                    } else if (deserializeMsg.hasJoin()) {
                        System.out.println("Join");
                        if (role == GameInfo.NodeRole.MASTER) {
                            boolean canJoin = Messages.RecvJoinMsg(model, curGameState.players.size() + 1);
                            if (canJoin) {
                                String name = deserializeMsg.getJoin().getPlayerName();
                                int id = AddNewPlayer(name, sender_addr, sender_port);
                                curGameState.snakes = model.getSnakes();
                                DatagramPacket datagramPacket = Messages.SendAckMsg(msg_seq_new, my_id, id, sender_addr, sender_port);
                                Messages.SendGameMsg(datagramPacket, socket);
                            } else {
                                Messages.SendErrorMsg();
                            }
                        }
                    } else if (deserializeMsg.hasSteer()) {
                        System.out.println("Steer");
                        if (role == GameInfo.NodeRole.MASTER) {
                            if (sender_addr.isLoopbackAddress()) {
                                sender_id = my_id;
                            } else {
                                GameInfo cur = curGameState;
                                for (int j = 0; j < cur.players.size(); ++j) {
                                    if (cur.players.get(j).getIp_address() == sender_addr && cur.players.get(j).getPort() == sender_port) {
                                        sender_id = cur.players.get(j).getId();
                                    }
                                }
                            }

                            SnakesProto.Direction direction = deserializeMsg.getSteer().getDirection();
                            if (!msg_seq_lastForPlayers.containsKey(sender_id) || msg_seq_new > msg_seq_lastForPlayers.get(sender_id)) {
                                System.out.println("update");
                                steersFromPlayers.put(sender_id, direction);
                                msg_seq_lastForPlayers.put(sender_id, msg_seq_new);
                            }
                        }
                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                    }
                    msg_seq_last = msg_seq_new;
                    msg_seq = msg_seq_last;

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
                if(role == GameInfo.NodeRole.MASTER) {
                    try {
                        Thread.sleep(1000);
                        ++msg_seq;
                        DatagramPacket datagram = Messages.SendAnnouncementMsg(curGameState, group, port, msg_seq);
                        Messages.SendGameMsg(datagram, socket);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
    class SenderGameState implements Runnable {
        @Override
        public void run(){
            //TODO этот цикл ЗАКАНЧИВАЕТСЯ, когда игра заканчивается!!!! while(gameIsPlaying) AtomicInt gameIsPlaying
            //а еще надо вызывать из DEPUTY этот поток
            int state_order = 0;
            while(true){
                if(role == GameInfo.NodeRole.MASTER) {
                    UpdateSteerMsgs();
                    curGameState.food = model.SpawnEat(curGameState.players.size());
                    for(int i = 0; i < curGameState.players.size(); ++i){
                        GameInfo.GamePlayer pl = curGameState.players.get(i);
                        if(pl.getId() != my_id){
                            DatagramPacket datagramPacket = Messages.SendStateMsg(msg_seq, pl.getIp_address(), pl.getPort(), curGameState, state_order);
                            Messages.SendGameMsg(datagramPacket, socket);
                            поместить в withoutAcks
                        }
                    }
                    ++state_order;
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

    class CheckerTime implements Runnable{
        @Override
        public void run(){
            while(true){
                try {
                    Thread.sleep((long)(config.getState_delay_ms() * 0.1));

                    for (ConcurrentHashMap.Entry<InetSocketAddress, Long> entry : timeLastMsg.entrySet()) {
                        InetSocketAddress key = entry.getKey();
                        Long value = entry.getValue();
                        if(System.currentTimeMillis() - value >= 0.8 * config.getState_delay_ms()){
                            ArrayList<GameInfo.GamePlayer> pl = curGameState.players;
                            for(int i = 0; i < pl.size(); ++i){
                                if(pl.get(i).getIp_address() == key.getAddress() && pl.get(i).getPort() == key.getPort()){
                                    if(role == GameInfo.NodeRole.MASTER){
                                        if(pl.get(i).getRole() == GameInfo.NodeRole.DEPUTY){
                                            if(pl.size() > 2) {
                                                curGameState.players.get(i + 1).setRole(GameInfo.NodeRole.DEPUTY);
                                                DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                                                        GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.DEPUTY.getTitle(),
                                                        my_id, pl.get(i).getRole().getTitle(),
                                                        pl.get(i).getPort(), pl.get(i).getIp_address());
                                                Messages.SendGameMsg(packet, socket);
                                                поместить в withoutAcks
                                            }
                                        }
                                    }
                                    else if(role == GameInfo.NodeRole.DEPUTY && pl.get(i).getRole() == GameInfo.NodeRole.MASTER){
                                        role = GameInfo.NodeRole.MASTER;
                                        curGameState.players.get(i).setRole(GameInfo.NodeRole.MASTER);
                                        try{
                                            byte[] buf = new String("Hello").getBytes();
                                            DatagramPacket d = new DatagramPacket(buf, buf.length);
                                            d.setAddress(InetAddress.getByName("127.0.0.1"));
                                            d.setPort(5555);
                                            socket.send(d);
                                        }
                                        catch (IOException ex){ex.printStackTrace();}
                                        myPort = socket.getLocalPort();

                                        model = new Model(config);
                                        model.setCurGameState();
                                        steersFromPlayers.clear();
                                        msg_seq_lastForPlayers.clear();
                                        try {
                                            master_ip = InetAddress.getByName("127.0.0.1");
                                            master_port = myPort;
                                        } catch (UnknownHostException e) {
                                            throw new RuntimeException(e);
                                        }

                                        for(int j = 0; j < pl.size(); ++j){
                                            if(j != i) {
                                                DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                                                        GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.NORMAL.getTitle(),
                                                        my_id, pl.get(j).getRole().getTitle(),
                                                        pl.get(j).getPort(), pl.get(j).getIp_address());
                                                Messages.SendGameMsg(packet, socket);
                                                поместить в withoutAcksYUY
                                            }
                                        }
                                    }
                                    else if(role == GameInfo.NodeRole.NORMAL && pl.get(i).getRole() == GameInfo.NodeRole.MASTER){
                                        for (GameInfo.GamePlayer gamePlayer : pl) {
                                            if (gamePlayer.getRole() == GameInfo.NodeRole.DEPUTY) {
                                                master_port = gamePlayer.getPort();
                                                master_ip = gamePlayer.getIp_address();
                                            }
                                        }
                                    }
                                    curGameState.players.remove(i);
                                }
                            }
                        }
                    }

                    for (ConcurrentHashMap.Entry<Long, DatagramPacket> entry : withoutAcks.entrySet()){
                        DatagramPacket value = entry.getValue();
                        Messages.SendGameMsg(value, socket);
                    }

                    if(System.currentTimeMillis() - timeMyLastMsg >= 0.1 * config.getState_delay_ms()){
                        Messages.SendPingMsg();
                        Messages.SendGameMsg();
                        параметры в пинг
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void UpdateInfo(){
        if(gwController != null && curGameState.snakes != null) {
            gwController.UpdateCanvas(curGameState);
            gwController.UpdateRecords();
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
        my_id = 1;
        model = new Model(conf);
        steersFromPlayers.clear();
        msg_seq_lastForPlayers.clear();
        System.out.println("CONFIG!!!!!  " + conf.getWidth());
        try {
            AddNewPlayer(myName, InetAddress.getByName("127.0.0.1"), myPort);
            master_ip = InetAddress.getByName("127.0.0.1");
            master_port = myPort;
            model.AddNewSnake(1);
        } catch(UnknownHostException ex){ex.printStackTrace();}
        curGameState.config = conf;
        config = conf;
        curGameState.snakes = model.getSnakes();

        setGameName();
    }

    private void SendAnswerAck(InetAddress sender_addr, int sender_port, long msg_seq_new){
        int id = 0;
        ArrayList<GameInfo.GamePlayer> pl = curGameState.players;
        for(int j = 0; j < pl.size(); ++j){
            if(pl.get(j).getIp_address() == sender_addr && pl.get(j).getPort() == sender_port){
                id = pl.get(j).getId();
                break;
            }
        }
        DatagramPacket datagramPacket = Messages.SendAckMsg(msg_seq_new, my_id, id, sender_addr, sender_port);
        Messages.SendGameMsg(datagramPacket, socket);
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
        msg_seq_last = -1;
    }
    private void setGameName(){
        curGameState.gameName = myName + "  " + String.valueOf(System.currentTimeMillis());
    }
    public boolean HasJoinOrError(){
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 2000){
            if(canJoin.get() != -1){
                break;
            }
        }
        return canJoin.get() == 1;
    }

    public void setNormal(){
        System.out.println("---------------------------------------");
        role = GameInfo.NodeRole.NORMAL;
    }

    public void SendSteerMsg(GameInfo.Direction direction){
        DatagramPacket datagramPacket = Messages.SendSteerMsg(direction, master_ip, master_port, msg_seq);
        Messages.SendGameMsg(datagramPacket, socket);
        поместить в withoutAcks
        ++msg_seq;
    }

    private Model model = null;
    private GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
    public volatile GameInfo curGameState = null;
    public GameInfo.GameConfig config = null;
    private String myName;
    private int my_id = 1;
    private int myPort = 0;
    public InetAddress master_ip = null;
    public int master_port = 0;
    private MulticastSocket socket = null;
    private GameWindowController gwController = null;
    private HashMap<Integer, SnakesProto.Direction> steersFromPlayers = new HashMap<>();
    private HashMap<Integer, Long> msg_seq_lastForPlayers = new HashMap<>();
    int port = 9192;
    InetAddress group = null;
    private long msg_seq = 0;
    private AtomicInteger canJoin = new AtomicInteger();
    long msg_seq_last = -1;
    /**
     * По msg_seq находится datagram,
     * на которую не был получен AckMsg и нужно повторить отправку
     * раз в 0.1 * state_delay_ms
     */
    ConcurrentHashMap<Long, DatagramPacket> withoutAcks = new ConcurrentHashMap<>();
    /**
     * По адресу отправителя последнее сообщение, которое он отправлял.
     * Если прошло более 0.8 * state_delay_ms => надо его выключить
     */
    ConcurrentHashMap<InetSocketAddress, Long> timeLastMsg = new ConcurrentHashMap<>();
    /**
     * Как давно я отправляла сообщения. Если прошло более 0.1 * state_delay_ms, нужно отправить PingMsg
     */
    long timeMyLastMsg = 0;
}
