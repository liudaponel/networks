package nsu.snake.peer;

import nsu.snake.model.Model;
import nsu.snake.peer.GameInfo;

import java.io.IOException;
import java.io.Reader;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import me.ippolitov.fit.snakes.SnakesProto;

public class Server implements Runnable {
    Server(String name){
        myName = name;
    }

    @Override
    public void run() {
        Thread reader = new Thread(new Reader());
        reader.start();
        //if(нажал создать игру - ты мастер)
        //из сообщения Ack из Join взять ip мастера
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            long msg_seq = 0; //номер сообщения
            int sender_id = 0; //(обязательно для AckMsg и RoleChangeMsg)
            int receiver_id = 11;

            DatagramSocket socket = null;
            InetAddress sender_addr = null;
            int sender_port = 0;
            try {
                socket = new DatagramSocket(5555);
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
                    SnakesProto.GameMessage deserializeMsg = SnakesProto.GameMessage.parseFrom(datagram.getData());

                    if (deserializeMsg.hasPing()) {
                        Messages.RecvPingMsg();
                    }
                    else if (deserializeMsg.hasAck()) {
                        Messages.RecvAckMsg();
                        if (msg_seq == 0) {
                            receiver_id = deserializeMsg.getReceiverId();
                        }
                    }
                    else if (deserializeMsg.hasError()) {
                        Messages.RecvErrorMsg();
                    }
                    else if (deserializeMsg.hasRoleChange()) {
                        //получается если я стал мастером, надо включить MasterWork и поменять у себя инфу
                        Messages.RecvRoleChangeMsg();
                    }
                    else if (deserializeMsg.hasState()) {
                        Messages.RecvStateMsg();
                    }
                    else if (deserializeMsg.hasJoin()) {
                        if(role == GameInfo.NodeRole.MASTER) {
                            boolean canJoin = Messages.RecvJoinMsg(model);
                            if(canJoin){
                                String name = deserializeMsg.getJoin().getPlayerName();
                                AddNewPlayer(name, sender_addr, sender_port);
                            }
                        }
                    }
                    msg_seq = deserializeMsg.getMsgSeq();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void MasterWork(){
        //тут будет в бесконечном цикле отправляться Announcement на мультикаст
        //и gameState тут же
        //TODO этот цикл ЗАКАНЧИВАЕТСЯ, когда игра заканчивается!!!! while(gameIsPlaying) AtomicInt gameIsPlaying
        //а еще надо это вызывать из DEPUTY
        if (curGameState == null){
            curGameState = new GameInfo();
            try {
                AddNewPlayer(myName, InetAddress.getByName("127.0.0.1"), 0);
            } catch(UnknownHostException ex){ex.printStackTrace();}
        }
        while(true){

        }
    }

    public void setIamMaster(GameInfo.GameConfig conf){
        role = GameInfo.NodeRole.MASTER;
        MasterWork();
        // TODO masterIp = my IP
        model = new Model(conf);
    }

    public GameInfo getCurState(){
        return null;
    }

    private void AddNewPlayer(String name, InetAddress ip_address, int port){
        GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
        if(curGameState.players.size() == 0){
            role = GameInfo.NodeRole.MASTER;
        }
        if(curGameState.players.size() == 1){
            role = GameInfo.NodeRole.DEPUTY;
        }
        int id = curGameState.players.size() + 1;
        int portTmp = ports[id % 5];
        GameInfo.GamePlayer newPlayer = new GameInfo.GamePlayer(name, id, ip_address, portTmp, role);
        curGameState.players.add(newPlayer);
    }

    private InetSocketAddress masterIp = null;
    private SocketChannel masterSocket = null;
    private Model model = null;
    GameInfo.NodeRole role = GameInfo.NodeRole.NORMAL;
    GameInfo curGameState = null;
    String myName;
    final int[] ports = new int[] {4444, 5555, 6666, 7777, 8888};
}
