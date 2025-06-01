package de.hexgame.logic;

// The target hexagon may be invalid for switching sides.
public record Move(Position targetHexagon) {
    public int getIndex() {
        return targetHexagon().getIndex();
    }

    @Override
    public String toString() {
        return targetHexagon.toString();
    }
}
