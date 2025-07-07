package de.hexgame.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Piece implements Cloneable {
    public enum Color {
        RED,
        BLUE;

        public Color invert() {
            return switch (this) {
                case RED -> BLUE;
                case BLUE -> RED;
            };
        }
    }
    private Color color;
    private boolean connectedLow;
    private boolean connectedHigh;

    public Piece(Color color) {
        this(color, false, false);
    }

    @Override
    public Piece clone() {
        try {
            return (Piece) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return color == piece.color;
    }
}
