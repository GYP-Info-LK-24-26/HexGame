package de.hexgame.logic;

public enum GameState {
    IN_PROGRESS,
    RED_WINS,
    BLUE_WINS;

    public static GameState getWinState(Piece.Color color) {
        if (color == Piece.Color.RED) {
            return GameState.RED_WINS;
        } else if (color == Piece.Color.BLUE) {
            return GameState.BLUE_WINS;
        } else {
            throw new IllegalArgumentException("Invalid color");
        }
    }
}
