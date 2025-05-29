package de.hexgame.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * This class may be run as a thread to play a game<br> the players have to be provided
 */
@AllArgsConstructor
public class Game extends Thread {
    public static final int VERSION = 7;
    @Getter
    private final GameState gameState;
    private final Player playerA;
    private final Player playerB;

    public Game(Player playerA, Player playerB) {
        this.gameState = new GameState();
        this.playerA = playerA;
        this.playerB = playerB;
    }

    @Override
    public void run() {
        Player playerToMove = playerA;
        Player otherPlayer = playerB;

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
        }

        System.out.printf("%s won!\n", otherPlayer.getName());
    }
}
