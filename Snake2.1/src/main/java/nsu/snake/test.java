package nsu.snake;
import me.ippolitov.fit.snakes.SnakesProto;
public class test {
    public static void main(String[] args) {
        System.out.println("hello");
        SnakesProto.GameMessage.Builder GameMsgBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.SteerMsg.Builder SteerMsgBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        SteerMsgBuilder.setDirection(SnakesProto.Direction.DOWN);
        GameMsgBuilder.setMsgSeq(7);
        GameMsgBuilder.setSteer(SteerMsgBuilder.build());
        SnakesProto.GameMessage gameMsg = GameMsgBuilder.build();

        byte[] serializedData = gameMsg.toByteArray();

        SnakesProto.GameMessage deserializedMessage= null;
        try {
            deserializedMessage = SnakesProto.GameMessage.parseFrom(serializedData);
        }
        catch(com.google.protobuf.InvalidProtocolBufferException ex){
            ex.printStackTrace();
        }

        System.out.println("Direction: " + deserializedMessage.getSteer().getDirection());
        System.out.println("MsgSeq: " + deserializedMessage.getMsgSeq());
    }
}
