package nsu.snake.peer;

import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.peer.GameInfo;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class CheckMulticast implements Runnable{
    @Override
    public void run() {
        Thread checker = new Thread(new CheckAnnouncementMsg());
        checker.start();
    }

    public HashMap<String, GameInfo> getCurGames(){
        return games;
    }

    private class CheckAnnouncementMsg implements Runnable{
        //  TODO сдесь просто бесконечно получается и обновляется информация. Есть какая-то инфа и я ее могу взять
        @Override
        public void run(){
            // TODO когда получаю сообщение, в GameInfo -> Players мастеру дописать его ip и port,
            // взятые из датаграммы, чтобы я потом в startWindow могла находить у мастера ip,port
            MulticastSocket socket = null;
            try {
                int port = 9192;
                InetAddress group = InetAddress.getByName("239.192.0.4");
//                int port = 8888;
//                InetAddress group = InetAddress.getByName("224.0.0.1");
                socket = new MulticastSocket(port);

                NetworkInterface networkInterface = findNetworkInterface("Realtek 8821CE Wireless LAN 802.11ac PCI-E NIC");
                if (networkInterface == null) {
                    System.err.println("Failed to find a suitable network interface");
                    return;
                }
                SocketAddress socketAddress = new InetSocketAddress(group, port);
                socket.joinGroup(socketAddress, networkInterface);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            while(true){
                byte buff[] = new byte[4096];
                DatagramPacket datagram = new DatagramPacket(buff, buff.length);
                try {
                    socket.receive(datagram);
//                    System.out.println("I receive Announcement");

                    byte[] bytes = new byte[datagram.getLength()];
                    System.arraycopy(datagram.getData(), 0, bytes, 0, datagram.getLength());
                    SnakesProto.GameMessage deserializeMsg = SnakesProto.GameMessage.parseFrom(bytes);

                    InetAddress sender_addr = datagram.getAddress();
                    int sender_port = datagram.getPort();
                    GameInfo gameState = new GameInfo();

                    String gameName = Messages.RecvAnnouncementMsg(gameState, sender_addr, sender_port, deserializeMsg);
                    games.put(gameName, gameState);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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

    /**
     * По уникальному названию игры информация о ней
     */
    HashMap<String, GameInfo> games = new HashMap<>();
}
