package nsu.snake.peer;

public interface Role {
    public void Reader(GameInfo.GameConfig config);
    public GameInfo getCurState();

    GameInfo.NodeRole role = null;
    GameInfo curGameInfo = null;
}
