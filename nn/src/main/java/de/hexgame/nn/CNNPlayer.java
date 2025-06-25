package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;
import de.hexgame.nn.mcts.GameTree;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@Slf4j
public class CNNPlayer implements Player {
    private static final int SIMULATION_COUNT = 400;

    private static int INSTANCE_COUNTER = 0;

    private final String name;
    private final Model model;
    private final GameTree gameTree;
    private final GameData gameData;

    public CNNPlayer(Model model, GameData gameData) {
        name = String.format("CNN Player %d", ++INSTANCE_COUNTER);
        this.model = model;
        gameTree = new GameTree(new GameState());
        this.gameData = gameData;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        gameTree.jumpTo(gameState);

        for (int i = 0; i < SIMULATION_COUNT; i++) {
            gameTree.expand(model);
        }

        Model.Output output = gameTree.getCombinedOutput().clone();
        try (INDArray probs = Nd4j.createFromArray(output.policy())) {

            for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
                if (!gameState.isLegalMove(new Move(new Position(i)))) {
                    probs.putScalar(i, -1e10);
                }
            }

            Transforms.softmax(probs, false);

            int targetIndex;
            if (gameData == null) {
                targetIndex = Nd4j.argMax(probs).getInt(0);
            } else {
                targetIndex = Nd4j.choice(Nd4j.arange(probs.length()), probs, 1).getInt(0);
                gameData.add(gameState.clone(), output);
            }
            return new Move(new Position(targetIndex));
        }
    }
}
