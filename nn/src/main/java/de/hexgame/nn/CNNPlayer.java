package de.hexgame.nn;

import de.hexgame.logic.GameState;
import de.hexgame.logic.Move;
import de.hexgame.logic.Player;
import de.hexgame.logic.Position;
import de.hexgame.nn.mcts.GameTree;
import de.hexgame.nn.mcts.TreeNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CNNPlayer implements Player {
    private static final File DEFAULT_MODEL_FILE = new File("saved_model");
    private static final int BATCH_SIZE = 16;

    private static final long[] TIME_BUDGETS = {200, 500, 1000, 2000, 4000, 8000};

    private static int INSTANCE_COUNTER = 0;

    private final String name;
    private final Model model;
    private final GameTree gameTree;
    private final long timeBudgetMs;

    public CNNPlayer(int difficulty) {
        name = String.format("CNN Player %d", ++INSTANCE_COUNTER);
        model = new Model(DEFAULT_MODEL_FILE);
        gameTree = new GameTree(new GameState());

        this.timeBudgetMs = TIME_BUDGETS[Math.max(0, Math.min(difficulty, TIME_BUDGETS.length - 1))];
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move think(GameState gameState) {
        gameTree.jumpTo(gameState);

        int completed = 0;
        long startMs = System.currentTimeMillis();
        while (startMs + timeBudgetMs > System.currentTimeMillis()) {
            List<TreeNode> leaves = new ArrayList<>();
            List<GameState> states = new ArrayList<>();

            for (int i = 0; i < BATCH_SIZE; i++) {
                TreeNode leaf = gameTree.selectLeaf();
                completed++;
                if (leaf != null) {
                    leaves.add(leaf);
                    states.add(leaf.getGameState());
                }
            }

            if (!leaves.isEmpty()) {
                List<Model.Output> outputs = model.predict(states);
                for (int i = 0; i < leaves.size(); i++) {
                    leaves.get(i).applyOutput(outputs.get(i));
                }
            }
        }

        Model.Output output = gameTree.getCombinedOutput();
        float[] policy = output.policy();
        int bestIndex = gameState.getLegalMoves().getFirst().getIndex();
        float maxValue = 0.0f;
        for (int i = 0; i < policy.length; i++) {
            if (policy[i] > maxValue) {
                maxValue = policy[i];
                bestIndex = i;
            }
        }

        System.out.println(completed);

        return new Move(new Position(bestIndex), Math.clamp(output.value(), -1.0f, 1.0f));
    }
}
