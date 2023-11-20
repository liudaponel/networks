package nsu.snake.peer;

import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Normal implements Role {
    @Override
    public void Reader(GameInfo.GameConfig config){
        GameInfo gameInfo;
        long msg_seq = 0; //номер сообщения
        int sender_id = 0; //(обязательно для AckMsg и RoleChangeMsg)
        int receiver_id = 11;

        DatagramSocket socket = null;
        InetAddress ip;
        try {
            socket = new DatagramSocket(5334);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        while(true){
            byte[] buff = new byte[1000];
            DatagramPacket sp1 = new DatagramPacket( buff, buff.length);
            try {
                socket.receive(sp1);
                SnakesProto.GameMessage deserialMsg = SnakesProto.GameMessage.parseFrom(sp1.getData());

                if (deserialMsg.hasPing()){
                    Messages.RecvPingMsg();
                }
                else if (deserialMsg.hasAck()) {
                    Messages.RecvAckMsg();
                    if(msg_seq == 0){
                        receiver_id = deserialMsg.getReceiverId();
                    }
                }
                else if (deserialMsg.hasError()) {
                    Messages.RecvErrorMsg();
                }
                else if (deserialMsg.hasRoleChange()){
                    //получается если я стал мастером, надо включить MasterWork и поменять у себя инфу
                    Messages.RecvRoleChangeMsg();
                }
                else if (deserialMsg.hasState()) {
                    Messages.RecvStateMsg();
                }
                msg_seq = deserialMsg.getMsgSeq();
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }

    @Override
    public GameInfo getCurState(){

        return null;
    }

    private GameInfo.NodeRole role;
    private GameInfo curGameInfo;
}
