package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;
import de.hexgame.nn.mcts.GameTree;
import lombok.val;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;

import static de.hexgame.logic.GameState.BOARD_SIZE;

public class CNNPlayer implements Player {
    private static int INSTANCE_COUNTER = 0;

    private final String name;
    private final Model model;
    private final GameTree gameTree;
    private final boolean train;

    public CNNPlayer(Model model, boolean train) {
        name = String.format("CNN Player %d", ++INSTANCE_COUNTER);
        this.model = model;
        gameTree = new GameTree(new GameState());
        this.train = train;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        gameTree.jumpTo(gameState);

        for (int i = 0; i < 400; i++) {
            gameTree.expand(model);
        }



        return null;
    }
}
