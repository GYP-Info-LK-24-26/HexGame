package de.hexgame.logic;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * this is one possibility to implement a Player<br>
 * this is the simplest of algorithms as it randomly picks a legal move
 */
public class RandomPlayer implements Player {
    private static int INSTANCE_COUNTER = 0;

    private final String name;

    public RandomPlayer() {
        name = String.format("Random Player %d", ++INSTANCE_COUNTER);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        List<Move> legalMoves = gameState.getLegalMoves();
        int randomIndex = ThreadLocalRandom.current().nextInt(legalMoves.size());
        return legalMoves.get(randomIndex);
    }

    @Override
    public void addPiece(Position position) {}
}
