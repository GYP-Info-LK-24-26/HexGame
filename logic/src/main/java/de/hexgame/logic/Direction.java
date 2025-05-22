package de.hexgame.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
}
