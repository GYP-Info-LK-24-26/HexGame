package de.hexgame.algorithm;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;


public class AlgorithmPlayer implements Player {

    private final Algorithm calculate;
    private final String name;

    public AlgorithmPlayer() {
        calculate = new Algorithm();
        name = "Algorithm Player";
    }

    public AlgorithmPlayer(String name) {
        calculate = new Algorithm();
        this.name = name;
    }

    public Position start() {
        Position temp;
        temp = calculate.longRowAlgorithm();
        return temp;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        calculate.setGameState(gameState);
        return new Move(start());
    }
}
