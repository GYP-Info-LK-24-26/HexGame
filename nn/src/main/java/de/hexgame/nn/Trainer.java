package de.hexgame.nn;

import de.hexgame.logic.GameState;
import lombok.val;
import org.deeplearning4j.nn.graph.ComputationGraph;

public class Trainer extends Thread {
    private static final int GAME_COUNT = 1000;

    private final ComputationGraph computationGraph;

    public Trainer(ComputationGraph computationGraph) {
        this.computationGraph = computationGraph;
    }

    @Override
    public void run() {
        val gameBoard = new GameState();
        for (int i = 0; i < GAME_COUNT; i++) {

            gameBoard.reset();
        }
    }
}
