package de.hexgame.nn;

import de.hexgame.logic.Game;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Player;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.dataset.MultiDataSet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Trainer implements Runnable {
    private static final int GAME_COUNT = 100;
    private static final Path MODEL_PATH = Path.of("model.zip");

    private final List<MultiDataSet> dataSetList = new ArrayList<>(2000);
    private final AtomicInteger gameCounter = new AtomicInteger(0);
    private Model model;

    @SneakyThrows
    @Override
    public void run() {
        if (Files.exists(MODEL_PATH)) {
            model = new Model(MODEL_PATH.toFile(), true);
        } else {
            model = new Model();
        }
        model.start();
        while (true) {
            log.info("Generating training data...");
            generateDataSet();
            log.info("Fitting the model...");
            fitModel();
            model.save(MODEL_PATH.toFile());
        }
    }

    @SneakyThrows
    private void generateDataSet() {
        gameCounter.set(0);
        final int threadCount = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(this::simulationTask);
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
    }

    private void fitModel() {
        model.fit(MultiDataSet.merge(dataSetList), 3);
    }

    @SneakyThrows
    private void simulationTask() {
        GameState gameState = new GameState();
        GameData gameDataA = new GameData();
        Player playerA = new CNNPlayer(model, gameDataA);
        GameData gameDataB = new GameData();
        Player playerB = new CNNPlayer(model, gameDataB);
        Game game = new Game(gameState, playerA, playerB);
        game.addPlayerWinListener(winner -> {
            synchronized (this) {
                gameDataA.extractDataSets(winner == playerA, model, dataSetList);
                gameDataB.extractDataSets(winner == playerB, model, dataSetList);
            }
        });

        int gameNumber;
        while ((gameNumber = gameCounter.incrementAndGet()) <= GAME_COUNT) {
            game.run();
            gameDataA.clear();
            gameDataB.clear();
            gameState.reset();
            log.info("Game {} has finished.", gameNumber);
        }
    }
}
