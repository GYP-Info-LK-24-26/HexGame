package de.hexgame.logic;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public record Position(int row, int column) {
    public Position(int hexagonIndex) {
        this(hexagonIndex / BOARD_SIZE, hexagonIndex % BOARD_SIZE);
    }

    public int getIndex() {
        if (!isValid()) throw new RuntimeException("Position is not valid");
        return row * BOARD_SIZE + column;
    }

    public boolean isValid() {
        return row >= 0 && row < BOARD_SIZE && column >= 0 && column < BOARD_SIZE;
    }

    public Position add(Direction direction) {
        return new Position(row + direction.getDeltaRow(), column + direction.getDeltaColumn());
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", row, column);
    }
}
