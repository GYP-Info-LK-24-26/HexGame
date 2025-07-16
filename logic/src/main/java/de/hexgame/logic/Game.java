package de.hexgame.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * This class may be run as a thread to play a game<br> the players have to be provided and most of the game logic except for player move querying is done in {@link GameState}
 */
@AllArgsConstructor
public class Game implements Runnable {
    public static final int VERSION = 10;
    @Getter
    private final GameState gameState;
    private final Player playerA;
    private final Player playerB;
    private List<PlayerWinListener> listeners;
    private List<WinChanceChangeListener> winChanceChangeListeners;
    private Thread runningThread;
    //keeps track of every listener hook
    private List<PlayerMoveListener> playerMoveListeners;
    private boolean terminated = false;

    /**
     * Converts this into a thread, calling this method on the same object will always yield the same thread<br>
     * if this method is never called no thread exists and the object can be used via its {@link Runnable} properties
     * @return a thread that is linked to this object
     */
    public Thread asThread(){
        if (runningThread == null) runningThread = new Thread(this);
        return runningThread;
    }

    /**
     * this adds a listener for player moves
     * @param playerMoveListener the listener
     */
    public void addPlayerMoveListener(PlayerMoveListener playerMoveListener) {
        if(playerMoveListeners != null)playerMoveListeners.add(playerMoveListener);
    }

    public Game(GameState gameState, Player playerA, Player playerB) {
        this.gameState = gameState;
        this.playerA = playerA;
        this.playerB = playerB;

        listeners = new ArrayList<>();
        winChanceChangeListeners = new ArrayList<>();
        playerMoveListeners = new ArrayList<>();
    }

    public Game(Player playerA, Player playerB) {
        this.gameState = new GameState();
        this.playerA = playerA;
        this.playerB = playerB;

        listeners = new ArrayList<>();
        winChanceChangeListeners = new ArrayList<>();
        playerMoveListeners = new ArrayList<>();
    }

    public void addPlayerWinListener(PlayerWinListener listener) {
        listeners.add(listener);
    }

    public void addWinChanceChangeListener(WinChanceChangeListener listener) {
        winChanceChangeListeners.add(listener);
    }

    @Override
    public void run() {
        Player playerToMove = playerA;
        Player otherPlayer = playerB;
        double playerToMoveChance = 0.0;
        double otherPlayerChance = 0.0;

        while (!gameState.isFinished() &&  !terminated) {
            Player finalPlayerToMove = playerToMove;
            playerMoveListeners.forEach(listener -> listener.onPlayerPreMove(finalPlayerToMove));
            Move move = playerToMove.think(gameState);
            if (!gameState.isLegalMove(move)) {
                throw new IllegalStateException(
                        String.format("%s tried to play the illegal move %s", playerToMove.getName(), move)
                );
            }
            gameState.makeMove(move);
            playerMoveListeners.forEach(listeners -> listeners.onPlayerMove(move.targetHexagon()));
            if(playerToMoveChance != move.winChance())winChanceChangeListeners.forEach(listeners -> listeners.onWinChanceChange(finalPlayerToMove,move.winChance()));


            Player tempPlayer = playerToMove;
            playerToMove = otherPlayer;
            otherPlayer = tempPlayer;

            playerToMoveChance = otherPlayerChance;
            otherPlayerChance = move.winChance();
        }

        if(gameState.isFinished()) {
            if (listeners.isEmpty()) System.out.printf("%s won!\n", otherPlayer.getName());
            else {
                final Player winner = otherPlayer;
                listeners.forEach(listeners -> listeners.onPlayerWin(winner));
            }
        }
    }

    /**
     * This makes the Game stop upon receiving the next move, used to forcefully end the game
     */
    public void terminate() {
        terminated = true;
    }
}
