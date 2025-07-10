package de.hexgame.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Describes all directions that are possible from a game piece, which are six in total named by their respective graphical relation
 */
@AllArgsConstructor
@Getter
public enum Direction {
    UP_LEFT(-1, 0),
    UP_RIGHT(-1, 1),
    RIGHT(0, 1),
    DOWN_RIGHT(1, 0),
    DOWN_LEFT(1, -1),
    LEFT(0, -1);

    public static final Direction[] ALL = Direction.values();

    private final int deltaRow, deltaColumn;

    public Direction changeDirection() {
        if (deltaRow == -1) {
            if (deltaColumn == 0) {
                return DOWN_RIGHT;
            }
            else {
                return DOWN_LEFT;
            }
        } else if (deltaRow == 0) {
            if (deltaColumn == -1) {
                return RIGHT;
            }
            else {
                return LEFT;
            }
        }
        else {
            if (deltaColumn == -1) {
                return UP_RIGHT;
            }
            else {
                return UP_LEFT;
            }
        }
    }
}
