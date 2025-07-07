package de.hexgame.logic;

/**
 * This provides all the data required to exert a move on the {@link GameState}
 * @param targetHexagon the hexagon that the move shall be done upon
 * @param winChance how big the chance is that the players wins, estimated by the player itself, may be Nan if it shall be invalid
 */
public record Move(Position targetHexagon,double winChance) {

    public Move(Position targetHexagon){
        this(targetHexagon, Double.NaN);
    }

    /**
     * same as {@link Position#getIndex()}
     */
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
