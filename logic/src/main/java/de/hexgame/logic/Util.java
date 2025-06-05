package de.hexgame.logic;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Util {

    /**
     * this is the base function to generate a random move
     * @param state the current {@link GameState} the move shall be generated for
     * @return one possible move where the chances of a specific move occurring are equally distributed over all possible moves
     */
    public static Move generateRandomMove(GameState state) {
        List<Move> legalMoves = state.getLegalMoves();
        int randomIndex = ThreadLocalRandom.current().nextInt(legalMoves.size());
        return legalMoves.get(randomIndex);
    }
}
