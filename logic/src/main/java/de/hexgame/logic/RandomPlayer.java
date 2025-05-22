package de.hexgame.logic;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    public Move think(GameBoard gameBoard) {
        List<Move> legalMoves = gameBoard.getLegalMoves();
        int randomIndex = ThreadLocalRandom.current().nextInt(legalMoves.size());
        return legalMoves.get(randomIndex);
    }
}
