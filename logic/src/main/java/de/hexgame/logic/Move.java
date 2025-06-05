package de.hexgame.logic;

// The target hexagon may be invalid for switching sides.
public record Move(Position targetHexagon,double winChance) {

    public Move(Position targetHexagon){
        this(targetHexagon, Double.NaN);
    }

    @Override
    public String toString() {
        if (targetHexagon.isValid()) {
            return String.format("{\ntarget=%s\nwin_chance%s\n}",targetHexagon,winChance);
        } else {
            return "(X)";
        }
    }
}
