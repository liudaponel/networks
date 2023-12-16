package nsu.snake.peer;

import javafx.scene.chart.PieChart;
import nsu.snake.model.Model;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
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
        canJoin.set(-1);
        try {
            group = InetAddress.getByName("239.192.0.4");
            socket = new MulticastSocket();
//            NetworkInterface networkInterface = findNetworkInterface("Realtek 8821CE Wireless LAN 802.11ac PCI-E NIC");
//            if (networkInterface == null) {
//                System.err.println("Failed to find a suitable network interface");
//                return;
//            }
//            socket.setNetworkInterface(networkInterface);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread reader = new Thread(new Reader());
        reader.start();
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
                    int sender_id2 = -100;
                    msg_seq_new = deserializeMsg.getMsgSeq();
                    int index_in_players = 0;
                    if (sender_addr.isLoopbackAddress()) {
                        sender_id = my_id;
                        sender_id2 = sender_id;
                    } else if (!deserializeMsg.hasAck() && !deserializeMsg.hasRoleChange()){
                        GameInfo cur = curGameState;
                        if(cur != null) {
                            for (int j = 0; j < cur.players.size(); ++j) {
                                if (cur.players.get(j).getIp_address() == sender_addr && cur.players.get(j).getPort() == sender_port) {
                                    sender_id = cur.players.get(j).getId();
                                    sender_id2 = sender_id;
                                    index_in_players = j;
                                    break;
                                }
                                if(sender_addr == master_ip && sender_port == master_port && cur.players.get(j).getRole() == GameInfo.NodeRole.MASTER){
                                    sender_id = cur.players.get(j).getId();
                                    sender_id2 = sender_id;
                                    index_in_players = j;
                                    break;
                                }
                            }
                        }
                    }

                    if(my_id != sender_id && sender_id2 != -100 && !deserializeMsg.hasJoin() && msg_seq_last != -1){
                        timeLastMsg.put(sender_id, System.currentTimeMillis());
                    }

                    if (deserializeMsg.hasPing()) {
                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                        System.out.println("Ping from " + sender_addr + ":" + sender_port);
                    } else if (deserializeMsg.hasAck()) {
                        System.out.println("Ack from " + sender_addr + ":" + sender_port + "  msg_seq: " + msg_seq_new);

                        if (msg_seq_last == -1) {
                            System.out.println("I received first ack");
                            receiver_id = deserializeMsg.getReceiverId();
                            my_id = receiver_id;
                            myPort = socket.getLocalPort();
                            master_ip = sender_addr;
                            master_port = sender_port;
                            canJoin.set(1);
                            StartGame(GameInfo.NodeRole.NORMAL);
                            curGameState.config = config;
                        }
                        else{
                            withoutAcks.remove(msg_seq_new);
                            withoutAcksLastTime.remove(msg_seq_new);
                            withoutAcksCounter.remove(msg_seq_new);
                            System.out.println("remove from withoutAcks  " + withoutAcks.size() + "  msg_seq was: " + msg_seq_new);
                            for (ConcurrentHashMap.Entry<Long, DatagramPacket> entry : withoutAcks.entrySet()) {
                                byte[] b = new byte[entry.getValue().getLength()];
                                System.arraycopy(entry.getValue().getData(), 0, b, 0, entry.getValue().getLength());
                                SnakesProto.GameMessage d = SnakesProto.GameMessage.parseFrom(b);
                                System.out.println("wwwwww  " + d.getMsgSeq());
                            }
                        }
                    } else if (deserializeMsg.hasError()) {
                        System.out.println("Error from " + sender_addr + ":" + sender_port);
                        msg_seq_last = -1;
                        canJoin.set(0);
                        Messages.RecvErrorMsg();
                    } else if (deserializeMsg.hasRoleChange()) {
                        System.err.println("RoleChange from " + deserializeMsg.getRoleChange().getSenderRole() +
                                " to:  " + deserializeMsg.getRoleChange().getReceiverRole() + " msg_seq: " + msg_seq_new);
                        if(role != GameInfo.NodeRole.MASTER){
                            if(deserializeMsg.getRoleChange().getReceiverRole() == SnakesProto.NodeRole.DEPUTY){
                                role = GameInfo.NodeRole.DEPUTY;
                            }
                            else if(deserializeMsg.getRoleChange().getReceiverRole() == SnakesProto.NodeRole.MASTER){
                                for(int i = 0; i < curGameState.players.size(); ++i) {
                                    if(curGameState.players.get(i).getId() == my_id) {
                                        MakeMeMaster(curGameState.players, i);
                                    }
                                    else if(curGameState.players.get(i).getRole() == GameInfo.NodeRole.MASTER){
                                        curGameState.players.get(i).setRole(GameInfo.NodeRole.VIEWER);
                                    }
                                }
                            }
                            else if(deserializeMsg.getRoleChange().getSenderRole() == SnakesProto.NodeRole.MASTER){
                                master_port = sender_port;
                                master_ip = sender_addr;
                                if(deserializeMsg.getRoleChange().hasReceiverRole()){
                                    role = RoleToRole(deserializeMsg.getRoleChange().getReceiverRole());
                                }
                            }
                        }

                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                    } else if (deserializeMsg.hasState()) {
                        System.out.println("State from " + sender_addr + ":" + sender_port);
                        Messages.RecvStateMsg(curGameState, deserializeMsg);
                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                    } else if (deserializeMsg.hasJoin()) {
                        System.out.println("Join from " + sender_addr + ":" + sender_port);
                        if (role == GameInfo.NodeRole.MASTER) {
                            boolean heWas = false;
                            for(GameInfo.GamePlayer pl: curGameState.players){
                                if(pl.getIp_address() == sender_addr && pl.getPort() == sender_port){
                                    heWas = true;
                                    break;
                                }
                            }

                            if(!heWas) {
                                int max = 1;
                                GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
                                for (GameInfo.GamePlayer pl : curGameState.players) {
                                    if (pl.getId() > max) {
                                        max = pl.getId();
                                    }
                                }
                                boolean canJoin = Messages.RecvJoinMsg(model, max + 1);
                                if (canJoin) {
                                    String name = deserializeMsg.getJoin().getPlayerName();
                                    if (curGameState.players.size() == 1) {
                                        role = GameInfo.NodeRole.DEPUTY;
                                    }
                                    int id = AddNewPlayer(name, sender_addr, sender_port, max, role);

                                    DatagramPacket datagramPacket = Messages.SendAckMsg(msg_seq_new, my_id, id, sender_addr, sender_port);
                                    Messages.SendGameMsg(datagramPacket, socket);

                                    if (role == GameInfo.NodeRole.DEPUTY) {
                                        SetNewDeputy(id, sender_port, sender_addr);
                                    }
                                    curGameState.snakes = model.getSnakes();

                                    timeMyLastMsg = System.currentTimeMillis();
                                } else {
                                    Messages.SendErrorMsg();
                                }
                            }
                        }
                    } else if (deserializeMsg.hasSteer()) {
                        System.out.println("Steer");
                        if (role == GameInfo.NodeRole.MASTER) {
                            SnakesProto.Direction direction = deserializeMsg.getSteer().getDirection();
                            if (!msg_seq_lastForPlayers.containsKey(sender_id) || msg_seq_new > msg_seq_lastForPlayers.get(sender_id)) {
                                System.out.println("update");
                                if(curGameState.players.get(index_in_players).getRole() != GameInfo.NodeRole.VIEWER) {
                                    steersFromPlayers.put(sender_id, direction);
                                }
                                msg_seq_lastForPlayers.put(sender_id, msg_seq_new);
                            }
                        }
                        SendAnswerAck(sender_addr, sender_port, msg_seq_new);
                    }
                    msg_seq_last = msg_seq_new;
                    msg_seq = msg_seq_last;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private GameInfo.NodeRole RoleToRole(SnakesProto.NodeRole role){
        switch (role){
            case NORMAL -> {
                return GameInfo.NodeRole.NORMAL;
            }
            case VIEWER -> {
                return GameInfo.NodeRole.VIEWER;
            }
            case DEPUTY -> {
                return GameInfo.NodeRole.DEPUTY;
            }
            case MASTER -> {
                return GameInfo.NodeRole.MASTER;
            }
        }
        return null;
    }

    class SenderAnnounce implements Runnable {
        @Override
        public void run(){
            //тут будет в бесконечном цикле отправляться Announcement на мультикаст
            while(gameIsPlaying && role == GameInfo.NodeRole.MASTER){
                try {
                    Thread.sleep(1000);
                    synchronized (lock) {
                        DatagramPacket datagram = Messages.SendAnnouncementMsg(curGameState, group, port, msg_seq);
                        Messages.SendGameMsg(datagram, socket);
                    }
                    timeMyLastMsg = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    class SenderGameState implements Runnable {
        @Override
        public void run(){
            int state_order = 0;
            while(gameIsPlaying){
                if(config != null) {
                    if (role == GameInfo.NodeRole.MASTER && model != null) {
                        UpdateSteerMsgs();
                        curGameState.food = model.SpawnEat(curGameState.players.size());
                        for (int i = 0; i < curGameState.players.size(); ++i) {
                            GameInfo.GamePlayer pl = curGameState.players.get(i);
                            if (pl.getId() != my_id) {
                                synchronized (lock) {
                                    ++msg_seq;
                                    DatagramPacket datagramPacket = Messages.SendStateMsg(msg_seq, pl.getIp_address(), pl.getPort(), curGameState, state_order);
                                    Messages.SendGameMsg(datagramPacket, socket);
                                    if(curGameState.players.size() > 1) {
                                        withoutAcks.put(msg_seq, datagramPacket);
                                        withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                                        withoutAcksCounter.put(msg_seq, 0);
                                    }
                                }
                                timeMyLastMsg = System.currentTimeMillis();
                            }
                        }
                        ++state_order;
                    }
                    UpdateInfo();
                    try {
                        Thread.sleep(config.getState_delay_ms());
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    class CheckerTime implements Runnable{
        @Override
        public void run(){
            ArrayList<Integer> toDelete = new ArrayList<>();
            while(gameIsPlaying) {
                if (config != null) {
                    try {
                        Thread.sleep((long)(0.01 * config.getState_delay_ms()));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    for (ConcurrentHashMap.Entry<Integer, Long> entry : timeLastMsg.entrySet()) {
                        GameInfo cur = curGameState;
                        InetAddress ip = null;
                        int port = 0;
                        int jj;
                        for (jj = 0; jj < cur.players.size(); ++jj) {
                            if (cur.players.get(jj).getId() == entry.getKey()) {
                                ip = cur.players.get(jj).getIp_address();
                                port = cur.players.get(jj).getPort();
                                break;
                            }
                        }
                        Long value = entry.getValue();

                        if (System.currentTimeMillis() - value >= 10 * config.getState_delay_ms() && !toDelete.contains(entry.getKey())) {
                            System.err.println("PLAYER DIED !!!!!!!!!!!!!!!!!!!");
                            ArrayList<GameInfo.GamePlayer> pl = curGameState.players;
                            for (int i = 0; i < pl.size(); ++i) {
                                if (pl.get(i).getIp_address() == ip && pl.get(i).getPort() == port) {
                                    System.out.println("heeeeeee is   " + pl.get(i).getRole()  + " and me  " + role);
                                    if (role == GameInfo.NodeRole.MASTER) {
                                        //мастер заметил, что отвалился зам и назначает нового зама
                                        System.out.println("111111111111111");
                                        int idToDel = pl.get(i).getId();
                                        MasterSetNewDeputy(pl, i);
                                        curGameState.players.remove(i);
                                        int j;
                                        GameInfo.Snake s = null;
                                        for(j = 0; j < curGameState.snakes.size(); ++j){
                                            if(curGameState.snakes.get(j).player_id == idToDel){
                                                s = curGameState.snakes.get(j);
                                                break;
                                            }
                                        }
                                        curGameState.snakes = model.DeleteSnake(s, j);
                                    }
                                    else if (role == GameInfo.NodeRole.DEPUTY && pl.get(i).getRole() == GameInfo.NodeRole.MASTER) {
                                        //зам заметил, что мастер отвалился и сам стал мастером
                                        System.out.println("222222222222");
                                        int idToDel = pl.get(i).getId();
                                        curGameState.players.remove(i);
                                        for(int k = 0; k < pl.size();++k){
                                            if(pl.get(k).getId() == my_id) {
                                                MakeMeMaster(pl, k);
                                            }
                                        }
                                        int j;
                                        GameInfo.Snake s = null;
                                        for(j = 0; j < curGameState.snakes.size(); ++j){
                                            if(curGameState.snakes.get(j).player_id == idToDel){
                                                s = curGameState.snakes.get(j);
                                                break;
                                            }
                                        }
                                        curGameState.snakes = model.DeleteSnake(s, j);
                                    }
                                    else if (role == GameInfo.NodeRole.NORMAL && pl.get(i).getRole() == GameInfo.NodeRole.MASTER) {
                                        //Обычный чел заметил, что отвалился мастер - теперь мастер это зам
                                        System.out.println("333333333333333");
                                        SetNewMasterFromNormal(pl);
                                        curGameState.players.remove(i);
                                    }
                                    break;
                                }
                            }
                            toDelete.add(entry.getKey());
                        }
                    }
                    for (Integer i : toDelete) {
                        timeLastMsg.remove(i);
                    }
                    toDelete.clear();

                    for (ConcurrentHashMap.Entry<Long, DatagramPacket> entry : withoutAcks.entrySet()) {
                        try {
                            Long key = entry.getKey();
                            int count = withoutAcksCounter.get(key);
                            if (System.currentTimeMillis() - withoutAcksLastTime.get(key) >= 0.1 * config.getState_delay_ms()) {
                                DatagramPacket value = entry.getValue();
                                Messages.SendGameMsg(value, socket);
                                withoutAcksCounter.put(key, count + 1);
                                withoutAcksLastTime.put(key, System.currentTimeMillis());
                                System.out.println("send msg again  " + count);
                            }
                            if (count == 5) {
                                withoutAcks.remove(key);
                                withoutAcksCounter.remove(key);
                                withoutAcksLastTime.remove(key);
                                System.out.println("I REMOVE   " + key);
                            }
                        }
                        catch(NullPointerException ex){
                            continue;
                        }
                    }

                    if (System.currentTimeMillis() - timeMyLastMsg >= 0.1 * config.getState_delay_ms()) {
                        synchronized (lock) {
                            ++msg_seq;
                            for (GameInfo.GamePlayer pl : curGameState.players) {
                                if (pl.getId() != my_id) {
                                    int port = pl.getPort();
                                    InetAddress addr = pl.getIp_address();
                                    if(pl.getRole() == GameInfo.NodeRole.MASTER){
                                        port = master_port;
                                        addr = master_ip;
                                    }
                                    DatagramPacket packet = Messages.SendPingMsg(msg_seq, port, addr);
                                    Messages.SendGameMsg(packet, socket);
                                    System.out.println("Send Ping");
                                }
                            }
                        }
                        timeMyLastMsg = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void MasterSetNewDeputy(ArrayList<GameInfo.GamePlayer> pl, int i){
        if (pl.get(i).getRole() == GameInfo.NodeRole.DEPUTY) {
            if (pl.size() > 2) {
                curGameState.players.get(i + 1).setRole(GameInfo.NodeRole.DEPUTY);
                SetNewDeputy(pl.get(i).getId(),pl.get(i).getPort(),  pl.get(i).getIp_address());
            }
        }

        int j;
        GameInfo.Snake s = null;
        for(j = 0; j < curGameState.snakes.size(); ++j){
            if(curGameState.snakes.get(j).player_id == pl.get(i).getId()){
                s = curGameState.snakes.get(j);
                break;
            }
        }
        curGameState.snakes = model.DeleteSnake(s, j);
    }

    private void MakeMeMaster(ArrayList<GameInfo.GamePlayer> pl, int i){
        System.out.println("--------- I am master -----------------");
        role = GameInfo.NodeRole.MASTER;
        curGameState.players.get(i).setRole(GameInfo.NodeRole.MASTER);
        try {
            byte[] buf = new String("Hello").getBytes();
            DatagramPacket d = new DatagramPacket(buf, buf.length);
            d.setAddress(InetAddress.getByName("127.0.0.1"));
            d.setPort(5555);
            socket.send(d);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        myPort = socket.getLocalPort();

        model = new Model(config);
        model.setCurGameState(curGameState);

        steersFromPlayers.clear();
        msg_seq_lastForPlayers.clear();
        try {
            master_ip = InetAddress.getByName("127.0.0.1");
            master_port = myPort;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        senderAnn = new Thread(new SenderAnnounce());
        senderAnn.start();

        for (int j = 0; j < pl.size(); ++j) {
            if (j != i && pl.get(j).getId() != my_id) {
                synchronized (lock) {
                    ++msg_seq;
                    DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                            GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.NORMAL.getTitle(),
                            my_id, pl.get(j).getId(),
                            pl.get(j).getPort(), pl.get(j).getIp_address());
                    Messages.SendGameMsg(packet, socket);
                    System.out.println("Now I am master, Send rolechange to all");
                    if(curGameState.players.size() > 1) {
                        withoutAcks.put(msg_seq, packet);
                        withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                        withoutAcksCounter.put(msg_seq, 0);
                    }
                }
                timeMyLastMsg = System.currentTimeMillis();
            }
        }
        setGameName();
    }

    private void SetNewMasterFromNormal(ArrayList<GameInfo.GamePlayer> pl){
        for (GameInfo.GamePlayer gamePlayer : pl) {
            if (gamePlayer.getRole() == GameInfo.NodeRole.DEPUTY) {
                master_port = gamePlayer.getPort();
                master_ip = gamePlayer.getIp_address();
            }
        }
    }

    private void SetNewDeputy(int id, int port, InetAddress ip){
        synchronized (lock) {
            ++msg_seq;
            DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                    GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.DEPUTY.getTitle(),
                    my_id, id,
                    port, ip);
            Messages.SendGameMsg(packet, socket);
            System.out.println("I set new deputy and send rolechange");
            if(curGameState.players.size() > 1) {
                withoutAcks.put(msg_seq, packet);
                withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                withoutAcksCounter.put(msg_seq, 0);
            }
        }
        timeMyLastMsg = System.currentTimeMillis();
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
            int senderPort = curGameState.players.get(i).getPort();
            InetAddress senderAddr = curGameState.players.get(i).getIp_address();
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
                int[] other_player_id = Messages.RecvSteerMsg(model, direction, sender_id);
                curGameState.snakes = model.getSnakes();

                if (other_player_id[0] != 0 && other_player_id.length == 1) {
                    int id = other_player_id[0];
                    if(other_player_id[0] < 0) id = (-1) * other_player_id[0];
                    for (int j = 0; j < curGameState.players.size(); ++j) {
                        if (id == curGameState.players.get(j).getId()) {
                            curGameState.players.get(j).setScore(curGameState.players.get(j).getScore() + 1);
                        }
                    }
                    if(sender_id != my_id && other_player_id[0] > 0) {
                        //не мастер врезался в кого-то
                        synchronized (lock) {
                            ++msg_seq;
                            DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                                    GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.VIEWER.getTitle(),
                                    my_id, sender_id,
                                    senderPort,
                                    senderAddr);
                            Messages.SendGameMsg(packet, socket);
                            if (curGameState.players.size() > 1) {
                                withoutAcks.put(msg_seq, packet);
                                withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                                withoutAcksCounter.put(msg_seq, 0);
                            }
                        }
                    }
                    else if (other_player_id[0] > 0){
                        // мастер врезался в кого-то
                        for (int j = 0; j < curGameState.players.size(); ++j) {
                            if (curGameState.players.get(j).getRole() == GameInfo.NodeRole.DEPUTY) {
                                synchronized (lock) {
                                    ++msg_seq;
                                    DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                                            GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.MASTER.getTitle(),
                                            my_id, curGameState.players.get(j).getId(),
                                            curGameState.players.get(j).getPort(),
                                            curGameState.players.get(j).getIp_address());
                                    Messages.SendGameMsg(packet, socket);
                                    curGameState.players.get(j).setRole(GameInfo.NodeRole.MASTER);
                                    master_port = curGameState.players.get(j).getPort();
                                    master_ip = curGameState.players.get(j).getIp_address();
                                    role = GameInfo.NodeRole.VIEWER;

                                    if (curGameState.players.size() > 1) {
                                        withoutAcks.put(msg_seq, packet);
                                        withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                                        withoutAcksCounter.put(msg_seq, 0);
                                    }
                                    System.out.println("------------------MASTER's SNAKE DIED------------------   " + msg_seq);
                                }
                                break;
                            }
                        }
                    }
                }
                else if(other_player_id.length == 2){
                    // два змеи столкнулись головами
                    for (int j = 0; j < curGameState.players.size(); ++j) {
                        for(int l = 0; l < 2; ++l) {
                            if (other_player_id[l] == curGameState.players.get(j).getId()) {
                                if(other_player_id[l] != my_id) {
                                    synchronized (lock) {
                                        ++msg_seq;
                                        DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                                                GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.VIEWER.getTitle(),
                                                my_id, other_player_id[0],
                                                curGameState.players.get(j).getPort(),
                                                curGameState.players.get(j).getIp_address());
                                        Messages.SendGameMsg(packet, socket);
                                        if (curGameState.players.size() > 1) {
                                            withoutAcks.put(msg_seq, packet);
                                            withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                                            withoutAcksCounter.put(msg_seq, 0);
                                        }
                                    }
                                }
                                else{
                                    synchronized (lock) {
                                        ++msg_seq;
                                        DatagramPacket packet = Messages.SendRoleChangeMsg(msg_seq,
                                                GameInfo.NodeRole.MASTER.getTitle(), GameInfo.NodeRole.MASTER.getTitle(),
                                                my_id, other_player_id[0],
                                                curGameState.players.get(j).getPort(),
                                                curGameState.players.get(j).getIp_address());
                                        Messages.SendGameMsg(packet, socket);
                                        role = GameInfo.NodeRole.VIEWER;
                                        curGameState.players.get(j).setRole(GameInfo.NodeRole.MASTER);
                                        master_port = curGameState.players.get(j).getPort();
                                        master_ip = curGameState.players.get(j).getIp_address();
                                        if (curGameState.players.size() > 1) {
                                            withoutAcks.put(msg_seq, packet);
                                            withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                                            withoutAcksCounter.put(msg_seq, 0);
                                        }
                                    }
                                }
                            }
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
        System.out.println("CONFIG!!!!!  " + conf.getState_delay_ms());
        try {
            AddNewPlayer(myName, InetAddress.getByName("127.0.0.1"), myPort, 0, GameInfo.NodeRole.MASTER);
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

    private int AddNewPlayer(String name, InetAddress ip_address, int port, int maxId, GameInfo.NodeRole role){
        if(curGameState.players.size() == 0){
            role = GameInfo.NodeRole.MASTER;
        }
        if(curGameState.players.size() == 1){
            role = GameInfo.NodeRole.DEPUTY;
        }
        int id = maxId + 1;
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
        synchronized (lock) {
            ++msg_seq;
            DatagramPacket datagramPacket = Messages.SendSteerMsg(direction, master_ip, master_port, msg_seq);
            Messages.SendGameMsg(datagramPacket, socket);
            if(curGameState.players.size() > 1) {
                withoutAcks.put(msg_seq, datagramPacket);
                withoutAcksLastTime.put(msg_seq, System.currentTimeMillis());
                withoutAcksCounter.put(msg_seq, 0);
            }
        }
        timeMyLastMsg = System.currentTimeMillis();
    }

    public void FinishGame(){
        gameIsPlaying = false;
        try {
            if(senderAnn != null) senderAnn.join();
            senderGS.join();
            checkTime.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("I EXIT GAME");
        senderAnn = null;
        senderGS = null;
        checkTime = null;
        curGameState = null;
    }

    public void StartGame(GameInfo.NodeRole role){
        System.out.println("START GAME");
        gameIsPlaying = true;
        curGameState = new GameInfo();
        if(role == GameInfo.NodeRole.MASTER) {
            this.role = GameInfo.NodeRole.MASTER;
            senderAnn = new Thread(new SenderAnnounce());
            senderAnn.start();
        }
        senderGS = new Thread(new SenderGameState());
        senderGS.start();
        checkTime = new Thread(new CheckerTime());
        checkTime.start();
    }

    public static NetworkInterface findNetworkInterface(String networkName) throws SocketException {
        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
//            System.out.println(iface.getDisplayName());
            if (iface.isUp() && !iface.isLoopback()) {
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof Inet4Address) {
//                        System.err.println(addr.getAddress() + "  " + iface.getDisplayName());
                        if (iface.getDisplayName().contains(networkName)) {
                            return iface;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Model model = null;
    private Thread senderAnn = null;
    private Thread senderGS = null;
    private Thread checkTime = null;
    private volatile boolean gameIsPlaying = false;
    private volatile GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
    public volatile GameInfo curGameState = null;
    public volatile GameInfo.GameConfig config = null;
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
    private static final Object lock = new Object();
    private AtomicInteger canJoin = new AtomicInteger();
    long msg_seq_last = -1;
    /**
     * По msg_seq находится datagram,
     * на которую не был получен AckMsg и нужно повторить отправку
     * раз в 0.1 * state_delay_ms
     */
    ConcurrentHashMap<Long, DatagramPacket> withoutAcks = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Integer> withoutAcksCounter = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Long> withoutAcksLastTime = new ConcurrentHashMap<>();
    /**
     * По адресу отправителя последнее сообщение, которое он отправлял.
     * Если прошло более 0.8 * state_delay_ms => надо его выключить
     */
    ConcurrentHashMap<Integer, Long> timeLastMsg = new ConcurrentHashMap<>();
    /**
     * Как давно я отправляла сообщения. Если прошло более 0.1 * state_delay_ms, нужно отправить PingMsg
     */
    private volatile long timeMyLastMsg = 0;
}
