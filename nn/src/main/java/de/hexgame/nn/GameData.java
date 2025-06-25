package de.hexgame.nn;

import de.hexgame.logic.GameState;
import org.nd4j.linalg.dataset.MultiDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameData {
    private final List<Map.Entry<GameState, Model.Output>> data = new ArrayList<>();

    public void add(GameState state, Model.Output output) {
        data.add(Map.entry(state, output));
    }

    public void extractDataSets(boolean hasWon, Model model, List<MultiDataSet> dataSetsOut) {
        data.forEach(entry -> {
            GameState gameState = entry.getKey();
            Model.Output output = entry.getValue();
            float targetValue = hasWon ? 1.0f : -1.0f;
            Model.Output targetOutput = new Model.Output(output.policy(), targetValue);
            dataSetsOut.add(model.createDataSet(gameState, targetOutput));
        });
    }

    public void clear() {
        data.clear();
    }
}
