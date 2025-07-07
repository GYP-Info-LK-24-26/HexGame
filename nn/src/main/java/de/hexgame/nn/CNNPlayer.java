package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;
import de.hexgame.nn.mcts.GameTree;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@Slf4j
public class CNNPlayer implements Player {
    private static final int SIMULATION_COUNT = 400;

    private static int INSTANCE_COUNTER = 0;

    private final String name;
    private final Model model;
    private final GameTree gameTree;
    private final GameData gameData;
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final Executor dispatcher = taskQueue::add;
    private int startedSimulations = 0;
    private int finishedSimulations = 0;

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

    @SneakyThrows
    @Override
    public Move think(GameState gameState) {
        startedSimulations = finishedSimulations = 0;

        gameTree.jumpTo(gameState);

        for (int i = 0; i < 16; i++) {
            expandGameTree();
        }

        while (finishedSimulations < SIMULATION_COUNT) {
            taskQueue.take().run();
        }

        Model.Output output = gameTree.getCombinedOutput();
        try (INDArray logits = Nd4j.createFromArray(output.policy())) {

            for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
                if (!gameState.isLegalMove(new Move(new Position(i)))) {
                    logits.putScalar(i, -1e10);
                }
            }

            int targetIndex;
            if (gameData == null) {
                targetIndex = Nd4j.argMax(logits).getInt(0);
            } else {
                Transforms.softmax(logits, false);
                targetIndex = Nd4j.choice(Nd4j.arange(logits.length()), logits, 1).getInt(0);
                gameData.add(gameState.clone(), output);
            }
            return new Move(new Position(targetIndex));
        }
    }

    private void expandGameTree() {
        gameTree.expand(model, dispatcher, gameData != null)
                .thenRunAsync(() -> {
                    finishedSimulations++;
                    if (startedSimulations < SIMULATION_COUNT) {
                        expandGameTree();
                    }
                }, dispatcher);
        startedSimulations++;
    }
}
