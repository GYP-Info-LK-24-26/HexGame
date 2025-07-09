package de.hexgame.nn;

import de.hexgame.logic.Game;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Player;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.deeplearning4j.datasets.iterator.file.FileMultiDataSetIterator;
import org.nd4j.linalg.dataset.MultiDataSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class Trainer implements Runnable {
    private static final int GAME_COUNT = 100;
    private static final Path DATA_DIRECTORY = Path.of("training-data");
    private static final Pattern MODEL_PATTERN = Pattern.compile("model-(\\d+).zip");
    private static final Pattern DATA_SET_PATTERN = Pattern.compile("dataset-(\\d+)");

    static {
        try {
            Files.createDirectories(DATA_DIRECTORY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final List<MultiDataSet> dataSetList = new ArrayList<>(2000);
    private final AtomicInteger gameCounter = new AtomicInteger(0);
    private Model model;
    private int modelNumber = 0;
    private int dataSetNumber = 0;
    private Path dataSetPath;

    @SneakyThrows
    @Override
    public void run() {
        try (Stream<Path> paths = Files.list(DATA_DIRECTORY)) {
            paths.forEach(file -> {
                String fileName = file.getFileName().toString();
                Matcher modelMatcher = MODEL_PATTERN.matcher(fileName);
                if (modelMatcher.matches()) {
                    int savedModelNumber = Integer.parseInt(modelMatcher.group(1));
                    if (savedModelNumber > modelNumber) {
                        modelNumber = savedModelNumber;
                    }
                }
                Matcher dataSetMatcher = DATA_SET_PATTERN.matcher(fileName);
                if (dataSetMatcher.matches() && FileUtils.listFiles(file.toFile(), null, false).size() >= 10) {
                    int savedDataSetNumber = Integer.parseInt(dataSetMatcher.group(1));
                    if (savedDataSetNumber > dataSetNumber) {
                        dataSetNumber = savedDataSetNumber;
                    }
                }
            });
        }
        if (modelNumber == 0) {
            model = new Model();
        } else {
            model = new Model(DATA_DIRECTORY.resolve(String.format("model-%d.zip", modelNumber)).toFile(), true);
        }
        if (dataSetNumber != 0) {
            dataSetPath = DATA_DIRECTORY.resolve(String.format("dataset-%d", dataSetNumber));
        }
        model.start();
        while (dataSetNumber < 10) {
            if (dataSetNumber <= modelNumber) {
                dataSetNumber++;
                dataSetPath = Files.createDirectories(DATA_DIRECTORY.resolve(String.format("dataset-%d", dataSetNumber)));
                log.info("Generating training data {}...", dataSetNumber);
                generateDataSet();
            }
            modelNumber++;
            log.info("Fitting the model {}...", modelNumber);
            fitModel();
            model.save(DATA_DIRECTORY.resolve(String.format("model-%d.zip", modelNumber)).toFile());
        }
    }

    @SneakyThrows
    private void generateDataSet() {
        gameCounter.set(FileUtils.listFiles(dataSetPath.toFile(), null, false).size() * 10);
        final int threadCount = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(this::simulationTask);
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        synchronized (this) {
            if (!dataSetList.isEmpty()) {
                MultiDataSet.merge(dataSetList).save(dataSetPath.resolve(UUID.randomUUID() + ".bin").toFile());
                dataSetList.clear();
            }
        }
    }

    private void fitModel() {
        model.fit(new FileMultiDataSetIterator(dataSetPath.toFile(), 128), 5);
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
            synchronized (this) {
                if (dataSetList.size() >= 1000) {
                    MultiDataSet.merge(dataSetList).save(dataSetPath.resolve(UUID.randomUUID() + ".bin").toFile());
                    dataSetList.clear();
                }
            }
        }
    }
}
