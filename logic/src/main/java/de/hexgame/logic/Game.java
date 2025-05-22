package de.hexgame.logic;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Game extends Thread {
    private final GameBoard gameBoard;
    private final Player playerA;
    private final Player playerB;

    @Override
    public void run() {
        Player playerToMove = playerA;
        Player otherPlayer = playerB;

        while (gameBoard.getGameState() == GameState.IN_PROGRESS) {
            Move move = playerToMove.think(gameBoard);
            if (!gameBoard.isLegalMove(move)) {
                throw new IllegalStateException(
                        String.format("%s tried to play the illegal move %s", playerToMove.getName(), move)
                );
            }
            gameBoard.makeMove(move);
            Player tempPlayer = playerToMove;
            playerToMove = otherPlayer;
            otherPlayer = tempPlayer;
        }

        System.out.printf("%s won!\n", otherPlayer.getName());
    }
}
