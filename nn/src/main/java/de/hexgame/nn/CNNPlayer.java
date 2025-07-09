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

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import static de.hexgame.logic.GameState.BOARD_SIZE;

@Slf4j
public class CNNPlayer implements Player {
    private static final File DEFAULT_MODEL_FILE = new File("training-data/model-1.zip");
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

    public CNNPlayer() {
        this(new Model(DEFAULT_MODEL_FILE, false), null);
        model.start();
    }

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
        gameState = gameState.cloneWithoutListeners();
        startedSimulations = finishedSimulations = 0;

        gameTree.jumpTo(gameState);

        for (int i = 0; i < 16; i++) {
            expandGameTree();
        }

        while (finishedSimulations < SIMULATION_COUNT) {
            taskQueue.take().run();
        }

        Model.Output output = gameTree.getCombinedOutput();
        float[] logitsJvm = output.policy();
        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            if (!gameState.isLegalMove(new Move(new Position(i)))) {
                logitsJvm[i] = -1e10f;
            }
        }
        try (INDArray logits = Nd4j.createFromArray(logitsJvm)) {
            int targetIndex;
            if (gameData == null) {
                targetIndex = Nd4j.argMax(logits).getInt(0);
            } else {
                Transforms.softmax(logits, false);
                targetIndex = Nd4j.choice(Nd4j.arange(logits.length()), logits, 1).getInt(0);
                gameData.add(gameState.clone(), new Model.Output(logits.toFloatVector(), output.value()));
            }
            return new Move(new Position(targetIndex), output.value());
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
