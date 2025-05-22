package de.hexgame.logic;

// The target hexagon may be invalid for switching sides.
public record Move(Position targetHexagon) {
    @Override
    public String toString() {
        if (targetHexagon.isValid()) {
            return targetHexagon.toString();
        } else {
            return "(X)";
        }
    }
}
