package de.hexgame.algorithm;

import de.hexgame.logic.*;

public class AlgorithmPlayer implements Player {

    private final Run calculate;
    private final int movesToCalculate;
    private final String name;

    public AlgorithmPlayer(int movesToCalculate) {
        calculate = new Run();
        this.movesToCalculate = movesToCalculate;
        name = "Algorithm Player";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        return new Move(calculate.start(gameState.getSideToMove(), movesToCalculate));
    }

    public void addPiece(Position position) {
        calculate.addPiece(position);
    }

}
