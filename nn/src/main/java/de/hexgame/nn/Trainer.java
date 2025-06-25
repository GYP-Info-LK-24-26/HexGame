package de.hexgame.nn;

import de.hexgame.logic.Game;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Player;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.dataset.MultiDataSet;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Trainer implements Runnable {
    private static final int GAME_COUNT = 1000;

    private final Model model;
    private final List<MultiDataSet> dataSets;
    private final AtomicInteger counter = new AtomicInteger(0);

    public Trainer(Model model) {
        this.model = model;
        dataSets = Collections.synchronizedList(new LinkedList<>());
    }

    @SneakyThrows
    @Override
    public void run() {
        log.info("Starting training...");
        counter.set(0);
        final int threadCount = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(this::simulationTask);
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        log.info("Saving data sets...");
        MultiDataSet dataSet = MultiDataSet.merge(dataSets);
        dataSet.save(new File("training.dat"));
    }

    private void simulationTask() {
        GameState gameState = new GameState();
        GameData gameDataA = new GameData();
        Player playerA = new CNNPlayer(model, gameDataA);
        GameData gameDataB = new GameData();
        Player playerB = new CNNPlayer(model, gameDataB);
        Game game = new Game(gameState, playerA, playerB);
        game.addPlayerWinListener(winner -> {
            gameDataA.extractDataSets(winner == playerA, model, dataSets);
            gameDataB.extractDataSets(winner == playerB, model, dataSets);
        });

        int gameIndex;
        while ((gameIndex = counter.incrementAndGet()) <= GAME_COUNT) {
            game.run();
            gameDataA.clear();
            gameDataB.clear();
            gameState.reset();
            log.info("Game {} has finished.", gameIndex);
        }
    }
}
