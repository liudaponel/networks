package nsu.snake.model;

import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import me.ippolitov.fit.snakes.SnakesProto;
import nsu.snake.peer.GameInfo;
import nsu.snake.peer.Messages;

import java.util.ArrayList;
import java.util.Random;

public class Model {
    public Model(GameInfo.GameConfig config){
        this.config = config;
        WIDTH = config.getWidth();
        HEIGHT = config.getHeight();
        field = new int[WIDTH * HEIGHT];
        for(int y = 0; y < HEIGHT; ++y){
            for(int x = 0; x < WIDTH; ++x){
                field[y*WIDTH + x] = 0;
                freeSquares.add(new GameInfo.Coord(x, y));
            }
        }
    }

    public void setCurGameState(GameInfo curState){
        //заполняем змей на поле
        snakes = new ArrayList<>(curState.snakes);
        for(int i = 0; i < curState.snakes.size(); ++i){
            GameInfo.Snake snake = curState.snakes.get(i);
            int x = 0;
            int y = 0;
            for(int j = 0; j < snake.points.size(); ++j) {
                int curX = snake.points.get(j).getX();
                int curY = snake.points.get(j).getY();

                x = ((curX + x) % WIDTH + WIDTH) % WIDTH;
                y = ((curY + y) % HEIGHT + HEIGHT) % HEIGHT;

                field[y * WIDTH + x] = snake.player_id;
            }
        }

        //добавляем на поле еду
        for(int i = 0; i < curState.food.size(); ++i){
            int x = curState.food.get(i).getX();
            int y = curState.food.get(i).getY();
            field[y * WIDTH + x] = -1;
            food.add(new GameInfo.Coord(x, y));
        }

        //оставшиеся клетки пустые
        for(int y = 0; y < HEIGHT; ++y){
            for(int x = 0; x < WIDTH; ++x){
                if(field[y * WIDTH + x] == 0) {
                    freeSquares.add(new GameInfo.Coord(x, y));
                }
            }
        }
    }

    public ArrayList<GameInfo.Coord> SpawnEat(int countPlayers){
        int needFood = config.getFood_static() + countPlayers - food.size();
        Random random = new Random();
        for(int i = 0; i < needFood; ++i){
            if(freeSquares.size() != 0){
                int rand = random.nextInt((freeSquares.size() - 1));
                field[freeSquares.get(rand).getY() * WIDTH + freeSquares.get(rand).getX()] = -1;
                food.add(freeSquares.get(rand));
                freeSquares.remove(rand);
            }
            else{
                break;
            }
        }
        return food;
    }
    public boolean AddNewSnake(int player_id){
        boolean canJoin = false;

        GameInfo.Coord head = null;
        GameInfo.Coord tail = null;
        GameInfo.Direction direction = null;
        for(int y = 0; y < HEIGHT; ++y){
            for(int x = 0; x < WIDTH; ++x){
                int sum = 0;
                for(int i = 0; i < 5; ++i){
                    for(int j = 0; j < 5; ++j){
                        int yy = (((y+i) % HEIGHT + HEIGHT) % HEIGHT);
                        int xx = (((x+j) % WIDTH + WIDTH) % WIDTH);
                        sum += field[yy * WIDTH + xx];
                    }
                }
                if(sum <= 0){
                    int xx = x + 2;
                    int yy = y + 2;
                    GameInfo.Coord tails[] = new GameInfo.Coord[4];
                    tails[0] = new GameInfo.Coord(1, 0);
                    tails[1] = new GameInfo.Coord(0, 1);
                    tails[2] = new GameInfo.Coord(- 1, 0);
                    tails[3] = new GameInfo.Coord(0, - 1);
                    GameInfo.Direction dirs[] = new GameInfo.Direction[4];
                    dirs[0] = GameInfo.Direction.LEFT;
                    dirs[1] = GameInfo.Direction.UP;
                    dirs[2] = GameInfo.Direction.RIGHT;
                    dirs[3] = GameInfo.Direction.DOWN;

                    Random random = new Random();
                    int i = random.nextInt(4);
                    if(field[(tails[i].getY() + yy) * WIDTH + tails[i].getX() + xx] == 0){
                        CreateNewSnake(xx, yy, tails, i, dirs, player_id);
                        canJoin = true;
                        break;
                    }
                    else{
                        i = random.nextInt(4);
                        if(field[(tails[i].getY() + yy) * WIDTH + tails[i].getX() + xx] == 0){
                            CreateNewSnake(xx, yy, tails, i, dirs, player_id);
                            canJoin = true;
                            break;
                        }
                    }
                }
            }
            if(canJoin) break;
        }
        return canJoin;
    }
    private void CreateNewSnake(int xx, int yy, GameInfo.Coord tails[], int i, GameInfo.Direction dirs[], int player_id){
        ArrayList<GameInfo.Coord> coords = new ArrayList<>();
        coords.add(new GameInfo.Coord(xx, yy));
        coords.add(new GameInfo.Coord(tails[i].getX(), tails[i].getY()));
        GameInfo.Snake newSnake = new GameInfo.Snake(player_id, coords, 0, dirs[i]);
        field[yy*WIDTH + xx] = player_id;

        field[(tails[i].getY() + yy) * WIDTH + tails[i].getX() + xx] = player_id;

        for(int j = 0; j < freeSquares.size(); ++j){
            if(     (freeSquares.get(j).getX() == xx && freeSquares.get(j).getY() == yy) ||
                    (freeSquares.get(j).getX() == (tails[i].getX() + xx) && freeSquares.get(j).getY() == (tails[i].getY()) + yy)){
                freeSquares.remove(j);
                break;
            }
        }

        snakes.add(newSnake);
    }

    public int[] MoveSnake(int player_id, SnakesProto.Direction direction){
        int other_player_id = 0;
        GameInfo.Direction direction2 = null;
        switch (direction){
            case UP -> direction2 = GameInfo.Direction.UP;
            case DOWN -> direction2 = GameInfo.Direction.DOWN;
            case LEFT -> direction2 = GameInfo.Direction.LEFT;
            case RIGHT -> direction2 = GameInfo.Direction.RIGHT;
        }

        GameInfo.Snake snake = null;
        int i = 0;
        for(i = 0; i < snakes.size(); ++i){
            if(snakes.get(i).player_id == player_id){
                snake = snakes.get(i);
                break;
            }
        }
        if(snake == null) return new int[2];

        int newHeadX = snake.points.get(0).getX();
        int newHeadY = snake.points.get(0).getY();
        switch (direction2){
            case UP -> {
                if(snake.head_direction == GameInfo.Direction.DOWN){
                    newHeadY = (((newHeadY + 1) % HEIGHT + HEIGHT) % HEIGHT );
                    direction2 = GameInfo.Direction.DOWN;
                }
                else{
                    newHeadY = (((newHeadY - 1) % HEIGHT + HEIGHT) % HEIGHT );
                }
            }
            case DOWN -> {
                if(snake.head_direction == GameInfo.Direction.UP){
                    newHeadY = (((newHeadY - 1) % HEIGHT + HEIGHT) % HEIGHT );
                    direction2 = GameInfo.Direction.UP;
                }
                else{
                    newHeadY = (((newHeadY + 1) % HEIGHT + HEIGHT) % HEIGHT );
                }
            }
            case RIGHT -> {
                if(snake.head_direction == GameInfo.Direction.LEFT){
                    newHeadX = (((newHeadX - 1) % WIDTH + WIDTH) % WIDTH );
                    direction2 = GameInfo.Direction.LEFT;
                }
                else{
                    newHeadX = (((newHeadX + 1) % WIDTH + WIDTH) % WIDTH );
                }
            }
            case LEFT -> {
                if(snake.head_direction == GameInfo.Direction.RIGHT){
                    newHeadX = (((newHeadX + 1) % WIDTH + WIDTH) % WIDTH );
                    direction2 = GameInfo.Direction.RIGHT;
                }
                else{
                    newHeadX = (((newHeadX - 1) % WIDTH + WIDTH) % WIDTH );
                }
            }
        }


        if(field[newHeadY * WIDTH + newHeadX] == 0){
            //значит поле было пустое
            MoveTail(snake);
            MoveHead(snake, direction2, newHeadX, newHeadY, 0);
            snake.head_direction = direction2;
            snakes.set(i, snake);

            for(int j = 0; j < freeSquares.size(); ++j){
                if(freeSquares.get(j).getX() == newHeadX && freeSquares.get(j).getY() == newHeadY){
                    freeSquares.remove(j);
                    field[newHeadY * WIDTH + newHeadX] = player_id;
                    break;
                }
            }
        }
        else if(field[newHeadY * WIDTH + newHeadX] == -1){
            //значит мы попали на еду
            MoveHead(snake, direction2, newHeadX, newHeadY, -1);
            snake.head_direction = direction2;
            snakes.set(i, snake);
            for(int j = 0; j < food.size(); ++j){
                if(food.get(j).getX() == newHeadX && food.get(j).getY() == newHeadY){
                    food.remove(j);
                    field[newHeadY * WIDTH + newHeadX] = player_id;
                    break;
                }
            }
            int ans[] = new int[1];
            ans[0] = (-1) * player_id;
            return ans;
        }
        else{
            //значит несколько змеек столкнулись
            for(int j = 0; j < snakes.size(); ++j){
                if(snakes.get(j).player_id == field[newHeadY * WIDTH + newHeadX]){
                    if(newHeadX == snakes.get(j).points.get(0).getX() && newHeadY == snakes.get(j).points.get(0).getY()){
                        DeleteSnake(snake, i);
                        DeleteSnake(snake, j);
                        int ans[] = new int[2];
                        ans[0] = snakes.get(j).player_id;
                        ans[1] = snakes.get(i).player_id;
                        return ans;
                    } else{
                        other_player_id = snakes.get(j).player_id;
                        DeleteSnake(snake, i);
                    }
                }
            }
            // TODO сделать ZOMBIE, пока что змея просто умирает
            //snake.state = 1;
        }
        int ans[] = new int[1];
        ans[0] = other_player_id;
        return ans;
    }

    public ArrayList<GameInfo.Snake> DeleteSnake(GameInfo.Snake snake, int i){
        Random random = new Random();

        int x = snake.points.get(0).getX();
        int y = snake.points.get(0).getY();
        boolean r = random.nextBoolean();
        if(r) field[y * WIDTH + x] = -1;
        else field[y * WIDTH + x] = 0;
        if(r){
            food.add(new GameInfo.Coord(x, y));
        }
        else{
            freeSquares.add(new GameInfo.Coord(x, y));
        }
        for(int j = 1; j < snake.points.size(); ++j){
            int curX = snake.points.get(j).getX();
            int curY = snake.points.get(j).getY();

            if(curX != 0) {
                for (int k = 1; k <= Math.abs(curX); ++k) {
                    r = random.nextBoolean();
                    int sign = 1;
                    if(curX < 0){
                        sign = -1;
                    }
                    int nx = ((x + sign * k) % WIDTH + WIDTH) % WIDTH;
                    int ny = (y % HEIGHT + HEIGHT) % HEIGHT;
                    if(r) {
                        field[ny * WIDTH + nx] = -1;
                        food.add(new GameInfo.Coord(nx, ny));
                    }
                    else {
                        field[ny * WIDTH + nx] = 0;
                        freeSquares.add(new GameInfo.Coord(nx, ny));
                    }
                }
            }
            if(curY != 0) {
                for (int k = 1; k <= Math.abs(curY); ++k) {
                    r = random.nextBoolean();
                    int sign = 1;
                    if(curY < 0){
                        sign = -1;
                    }
                    int nx = (x % WIDTH + WIDTH) % WIDTH;
                    int ny = ((y + sign * k)% HEIGHT + HEIGHT) % HEIGHT;
                    if(r) {
                        field[ny * WIDTH + nx] = -1;
                        food.add(new GameInfo.Coord(nx, ny));
                    }
                    else {
                        field[ny * WIDTH + nx] = 0;
                        freeSquares.add(new GameInfo.Coord(nx, ny));
                    }
                }
            }

            x += curX;
            y += curY;
        }
        snakes.remove(i);
        return snakes;
    }

    private void MoveTail(GameInfo.Snake snake){
        int tailX = snake.points.get(snake.points.size() - 1).getX();
        int tailY = snake.points.get(snake.points.size() - 1).getY();

        //считаю координаты последней клетки
        GameInfo.Coord oldSnake = new GameInfo.Coord(snake.points.get(0).getX(), snake.points.get(0).getY());
        for(int i = 1; i < snake.points.size(); ++i){
            oldSnake.setX(oldSnake.getX() + snake.points.get(i).getX());
            oldSnake.setY(oldSnake.getY() + snake.points.get(i).getY());
        }
        oldSnake.setX((oldSnake.getX() % WIDTH + WIDTH) % WIDTH);
        oldSnake.setY((oldSnake.getY() % HEIGHT + HEIGHT) % HEIGHT);
        freeSquares.add(oldSnake);
        field[oldSnake.getY() * WIDTH + oldSnake.getX()] = 0;
        snake.points.remove(snake.points.size() - 1);

        int newTailX = 0;
        int newTailY = 0;
        if (tailX != 0) {
            if (tailX < -1) {
                newTailX = tailX + 1;
            } else if (tailX > 1) {
                newTailX = tailX - 1;
            }
        } else if (tailY != 0) {
            if (tailY < -1) {
                newTailY = tailY + 1;
            } else if (tailY > 1) {
                newTailY = tailY - 1;
            }
        }
        if (newTailX != 0 || newTailY != 0) {
            snake.points.add(new GameInfo.Coord(newTailX, newTailY));
        }
    }

    private void MoveHead(GameInfo.Snake snake, GameInfo.Direction direction2, int newHeadX, int newHeadY, int field){
        snake.points.set(0, new GameInfo.Coord(newHeadX, newHeadY));
        if(direction2 == snake.head_direction && snake.points.size() != 1){
            int x = snake.points.get(1).getX();
            int y = snake.points.get(1).getY();
            if(x > 0) ++x;
            if(x < 0) --x;
            if(y < 0) --y;
            if(y > 0) ++y;
            snake.points.set(1, new GameInfo.Coord(x, y));
        } else{
            switch (direction2){
                case UP -> snake.points.add(1, new GameInfo.Coord(0, 1));
                case DOWN -> snake.points.add(1, new GameInfo.Coord(0, -1));
                case RIGHT -> snake.points.add(1, new GameInfo.Coord(-1, 0));
                case LEFT -> snake.points.add(1, new GameInfo.Coord(1, 0));
            }
        }
    }

    public ArrayList<GameInfo.Snake> getSnakes(){
        return snakes;
    }
    private GameInfo.GameConfig config;
    private int field[];
    private int WIDTH, HEIGHT;
    private ArrayList<GameInfo.Snake> snakes = new ArrayList<>();
    private ArrayList<GameInfo.Coord> freeSquares = new ArrayList<>();
    private ArrayList<GameInfo.Coord> food = new ArrayList<>();
}
