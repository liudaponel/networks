package nsu.snake.model;

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
            }
        }
    }
    public boolean AddNewSnake(){
        boolean canJoin = false;

        GameInfo.Coord head = null;
        GameInfo.Coord tail = null;
        GameInfo.Direction direction = null;
        for(int y = 0; y < HEIGHT; ++y){
            for(int x = 0; x < WIDTH; ++x){
                int sum = 0;
                for(int i = 0; i < 5; ++i){
                    for(int j = 0; j < 5; ++j){
                        int yy = ((y+i) % HEIGHT);
                        int xx = ((x+j) % WIDTH);
                        sum += field[yy * HEIGHT + xx];
                    }
                }
                if(sum <= 0){
                    int xx = x + 2;
                    int yy = y + 2;
                    GameInfo.Coord tails[] = new GameInfo.Coord[4];
                    tails[0] = new GameInfo.Coord(xx + 1, yy);
                    tails[1] = new GameInfo.Coord(xx, yy + 1);
                    tails[2] = new GameInfo.Coord(xx - 1, yy);
                    tails[3] = new GameInfo.Coord(xx, yy - 1);
                    GameInfo.Direction dirs[] = new GameInfo.Direction[4];
                    dirs[0] = GameInfo.Direction.LEFT;
                    dirs[1] = GameInfo.Direction.UP;
                    dirs[2] = GameInfo.Direction.RIGHT;
                    dirs[3] = GameInfo.Direction.DOWN;

                    Random random = new Random();
                    int i = random.nextInt(4);
                    if(field[tails[i].getY() * HEIGHT + tails[i].getX()] == 0){
                        CreateNewSnake(xx, yy, tails, i, dirs);
                        canJoin = true;
                        break;
                    }
                    else{
                        i = random.nextInt(4);
                        if(field[tails[i].getY() * HEIGHT + tails[i].getX()] == 0){
                            CreateNewSnake(xx, yy, tails, i, dirs);
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
    private void CreateNewSnake(int xx, int yy, GameInfo.Coord tails[], int i, GameInfo.Direction dirs[]){
        ArrayList<GameInfo.Coord> coords = new ArrayList<>();
        coords.add(new GameInfo.Coord(xx, yy));
        coords.add(new GameInfo.Coord(tails[i].getX(), tails[i].getY()));
        GameInfo.Snake newSnake = new GameInfo.Snake(0, coords, 0, dirs[i]);

        snakes.add(newSnake);
    }

    private GameInfo.GameConfig config;
    private int field[];
    private int WIDTH, HEIGHT;
    private ArrayList<GameInfo.Snake> snakes = new ArrayList<>();
}
