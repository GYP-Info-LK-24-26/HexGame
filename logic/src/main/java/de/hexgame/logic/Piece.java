package de.hexgame.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Piece {
    public enum Color {
        RED,
        BLUE
    }

    private Color color;
    private boolean connectedLow;
    private boolean connectedHigh;

    public Piece(Color color) {
        this(color, false, false);
    }
}
