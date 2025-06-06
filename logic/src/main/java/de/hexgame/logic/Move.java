package de.hexgame.logic;

// The target hexagon may be invalid for switching sides.
public record Move(Position targetHexagon,double winChance) {

    public Move(Position targetHexagon){
        this(targetHexagon, Double.NaN);
    }

    public int getIndex() {
        return targetHexagon().getIndex();
    }

    @Override
    public String toString() {
        if (targetHexagon.isValid()) {
            return targetHexagon.toString();
        } else {
            return "(X)";
        }
    }
}
