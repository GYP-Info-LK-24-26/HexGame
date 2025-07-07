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

    /**
     * Converts this into a thread, calling this method on the same object will always yield the same thread<br>
     * if this method is never called no thread exists and the object can be used via its {@link Runnable} properties
     * @return a thread that is linked to this object
     */
    public Thread asThread(){
        if (runningThread == null) runningThread = new Thread(this);
        return runningThread;
    }

    public Game(GameState gameState, Player playerA, Player playerB) {
        this.gameState = gameState;
        this.playerA = playerA;
        this.playerB = playerB;

        listeners = new ArrayList<>();
        winChanceChangeListeners = new ArrayList<>();
    }

    public Game(Player playerA, Player playerB) {
        this.gameState = new GameState();
        this.playerA = playerA;
        this.playerB = playerB;

        listeners = new ArrayList<>();
        winChanceChangeListeners = new ArrayList<>();
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

        while (!gameState.isFinished()) {
            Move move = playerToMove.think(gameState);
            if (!gameState.isLegalMove(move)) {
                throw new IllegalStateException(
                        String.format("%s tried to play the illegal move %s", playerToMove.getName(), move)
                );
            }
            gameState.makeMove(move);
            Player tempPlayer = playerToMove;
            playerToMove = otherPlayer;
            otherPlayer = tempPlayer;

            final Player player = playerToMove;
            if(playerToMoveChance != move.winChance())winChanceChangeListeners.forEach(listeners -> listeners.onWinChangeChange(player,move.winChance()));

            playerToMoveChance = otherPlayerChance;
            otherPlayerChance = move.winChance();
        }

        if(listeners.isEmpty()) System.out.printf("%s won!\n", otherPlayer.getName());
        else{
            final Player winner = otherPlayer;
            listeners.forEach(listeners -> listeners.onPlayerWin(winner));
        }
    }
}
