package de.hexgame.nn.training;

import de.hexgame.logic.GameState;
import de.hexgame.nn.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameData {
    private final List<DataPoint> data = new ArrayList<>();

    public void add(GameState state, Model.Output output) {
        data.add(new DataPoint(state, output));
    }

    public void extractSamples(boolean hasWon, Consumer<ExperienceBuffer.Sample> samplesOut) {
        data.forEach(entry -> {
            GameState gameState = entry.state();
            Model.Output output = entry.output();
            float targetValue = hasWon ? 1.0f : -1.0f;
            Model.Output targetOutput = new Model.Output(output.policy(), targetValue);
            samplesOut.accept(new ExperienceBuffer.Sample(gameState, targetOutput));
        });
    }

    public void clear() {
        data.clear();
    }

    private record DataPoint(GameState state, Model.Output output) {
    }
}
