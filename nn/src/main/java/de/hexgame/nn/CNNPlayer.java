package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;
import de.hexgame.nn.mcts.GameTree;
import de.hexgame.nn.training.GameData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class CNNPlayer implements Player {
    private static final File DEFAULT_MODEL_FILE = new File("model.zip");
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
        gameTree = new GameTree(new GameState(), gameData != null);
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

        // Execute tasks until all simulations are finished
        while (finishedSimulations < SIMULATION_COUNT) {
            taskQueue.take().run();
        }

        Model.Output output = gameTree.getCombinedOutput();
        float[] policy = output.policy();
        int targetIndex = -1;
        // Check if collecting training data
        if (gameData == null) {
            // Select best move
            float maxValue = 0.0f;
            for (int i = 0; i < policy.length; i++) {
                if (policy[i] > maxValue) {
                    maxValue = policy[i];
                    targetIndex = i;
                }
            }
        } else {
            // Normalize policy
            float sum = 0.0f;
            for (float v : policy) {
                sum += v;
            }
            for (int i = 0; i < policy.length; i++) {
                policy[i] /= sum;
            }

            // Sample random move based on policy
            float randomValue = ThreadLocalRandom.current().nextFloat();
            for (int i = 0; i < policy.length; i++) {
                randomValue -= policy[i];
                if (randomValue < 1e-6) {
                    targetIndex = i;
                }
            }
            gameData.add(gameState.clone(), new Model.Output(policy, output.value()));
        }

        return new Move(new Position(targetIndex), output.value());
    }

    private void expandGameTree() {
        gameTree.expand(model, dispatcher)
                .thenRunAsync(() -> {
                    finishedSimulations++;
                    if (startedSimulations < SIMULATION_COUNT) {
                        expandGameTree();
                    }
                }, dispatcher);
        startedSimulations++;
    }
}
