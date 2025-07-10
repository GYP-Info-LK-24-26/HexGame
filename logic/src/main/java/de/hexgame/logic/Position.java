package de.hexgame.logic;

import static de.hexgame.logic.GameState.BOARD_SIZE;

/**
 * This defines a position of on the game board in row-column order
 * @param row the row of the object
 * @param column the column of the object
 */
public record Position(int row, int column) {
    public Position(int hexagonIndex) {
        this(hexagonIndex / BOARD_SIZE, hexagonIndex % BOARD_SIZE);
    }

    /**
     * creates the single integer index that may be used for array addressing
     * @return a single integer index
     */
    public int getIndex() {
        if (!isValid()) throw new RuntimeException("Position" + toString() + " is invalid");
        return row * BOARD_SIZE + column;
    }

    /**
     * Checks for validity
     * @return if the position is within bounds
     */
    public boolean isValid() {
        return row >= 0 && row < BOARD_SIZE && column >= 0 && column < BOARD_SIZE;
    }

    /**
     * creates a new Position which is moved by one of the six directions
     * @param direction the direction to add
     * @return a new Position
     * @see Direction
     */
    public Position add(Direction direction) {
        return new Position(row + direction.getDeltaRow(), column + direction.getDeltaColumn());
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", row, column);
    }
}
