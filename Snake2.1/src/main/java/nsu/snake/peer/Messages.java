package nsu.snake.peer;

import nsu.snake.model.Model;

public class Messages {
    public static void SendAnnouncementMsg(){

    }

    public static void SendSteerMsg(){
        //NORMAL
    }

    public static void RecvSteerMsg(){
        //MASTER
    }

    public static void SendAckMsg(){
        //NORMAL and MASTER каждый раз при подтверждении
    }

    public static void RecvAckMsg(){
        //NORMAL только при подключении, либо на ответ мастеру
        //MASTER каждый раз при подтверждении сообщения

    }

    public static void SendStateMsg(){
        //MASTER
    }

    public static void RecvStateMsg(){
        //NORMAL AND MASTER (он тоже должен менять состояние)
    }

    public static void SendRoleChangeMsg(){
        //хз там надо разбираться
    }

    public static void RecvRoleChangeMsg(){

    }

    public static void SendPingMsg(){
        //MASTER
    }

    public static void RecvPingMsg(){
        //NORMAL
    }

    public static void RecvErrorMsg(){
        //NORMAL
    }

    public static void SendErrorMsg(){
        //MASTER
    }

    public static boolean SendJoinMsg(){
        //NORMAL OR MASTER (только в начале)
        //        Для присоединения к игре отправляется сообщение JoinMsg узлу, от которого было получено сообщение AnnouncementMsg.
//                В этом сообщении указывается имя игры, к которой хотим присоединиться (в текущей версии задачи это неважно, оставлено на будущее).
//                Также указывается режим присоединения, стандартный или "только просмотр", во втором случае игрок не получает змейку, а может только просматривать, что происходит на поле.
        return false;
    }

    public static boolean RecvJoinMsg(Model model){
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
        boolean canJoin = model.AddNewSnake();
        if(canJoin){
            SendAckMsg();
        }
        else{
            SendErrorMsg();
        }
        return canJoin;
    }
}
