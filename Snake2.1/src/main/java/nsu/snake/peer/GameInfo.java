package nsu.snake.peer;

import java.net.InetAddress;
import java.util.ArrayList;

public class GameInfo {
    public static class GameConfig {
        public GameConfig(int width, int height, int food_static, int state_delay_ms){
            this.width = width;
            this.height = height;
            this.food_static = food_static;
            this.state_delay_ms = state_delay_ms;
        }
        public int getWidth() {
            return width;
        }
        public int getHeight() {
            return height;
        }
        public int getFood_static() {
            return food_static;
        }
        public int getState_delay_ms() {
            return state_delay_ms;
        }

        private int width = 40;
        private int height = 30;
        private int food_static = 1;
        private int state_delay_ms = 1000;
    }
    public enum NodeRole {
        NORMAL(0), // Обычный узел, лист в топологии "звезда"
        MASTER(1), // Главный узел, центр в топологии "звезда"
        DEPUTY(2), // Заместитель главного узла
        VIEWER(3); // Наблюдатель, похож на NORMAL, но не имеет змеи в статусе ALIVE, только получает обновления статуса
        private int title;
        NodeRole(int title) {
            this.title = title;
        }
        public int getTitle() {
            return title;
        }
    }
    public static class GamePlayer {
        public GamePlayer(String name, int id, InetAddress ip_address, int port, NodeRole role){
            this.id = id;
            this.name = name;
            this.port = port;
            this.ip_address = ip_address;
            this.role = role;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public InetAddress getIp_address() {
            return ip_address;
        }
        public void setIp_address(InetAddress ip_address) {
            this.ip_address = ip_address;
        }
        public int getPort() {
            return port;
        }
        public void setPort(int port) {
            this.port = port;
        }
        public NodeRole getRole() {
            return role;
        }
        public void setRole(NodeRole role) {
            this.role = role;
        }
        public int getType() {
            return type;
        }
        public void setType(int type) {
            this.type = type;
        }
        public int getScore() {
            return score;
        }
        public void setScore(int score) {
            this.score = score;
        }

        private String name;
        private int id;
        private InetAddress ip_address;
        private int port;
        private NodeRole role;
        /**
         * 0 - HUMAN, 1 - ROBOT
         */
        private int type = 0;
        private int score = 0;
    };
    public static class Coord {
        public Coord(int x, int y){
            this.x = x;
            this.y = y;
        }
        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }
        public int getY() {
            return y;
        }
        public void setY(int y) {
            this.y = y;
        }

        private int x = 0; // По горизонтальной оси, положительное направление - вправо
        private int y = 0; // По вертикальной оси, положительное направление - вниз

    }
    public enum Direction {
        UP(1),     // Вверх (в отрицательном направлении оси y)
        DOWN(2),   // Вниз (в положительном направлении оси y)
        LEFT(3),   // Влево (в отрицательном направлении оси x)
        RIGHT(4);  // Вправо (в положительном направлении оси x)
        private int title;
        Direction(int title) {
            this.title = title;
        }
        public int getTitle() {
            return title;
        }
    }
    public static class Snake {
        public Snake(int player_id, ArrayList<Coord> points, int state, Direction head_direction){
            this.player_id = player_id;
            this.points = points;
            this.state = state;
            this.head_direction = head_direction;
        }
        public int player_id = 1;
        /** Список "ключевых" точек змеи. Первая точка хранит координаты головы змеи.
         * Каждая следующая - смещение следующей "ключевой" точки относительно предыдущей,
         * в частности последняя точка хранит смещение хвоста змеи относительно предыдущей "ключевой" точки. */
        public ArrayList<Coord> points;
        /**
         *  0 - ALIVE, 1 - ZOMBIE
         *  */
        public int state = 0;
        /**
         * Направление, в котором "повёрнута" голова змейки в текущий момент
         */
        public Direction head_direction = Direction.RIGHT;
    }

    public GameConfig config;
    public String gameName;
    public ArrayList<GamePlayer> players = new ArrayList<>();
    public ArrayList<Snake> snakes;
    public ArrayList<Coord> food;
}
