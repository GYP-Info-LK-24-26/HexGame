package de.hexgame.logic;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Game extends Thread {
    private final GameState gameState;
    private final Player playerA;
    private final Player playerB;

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
