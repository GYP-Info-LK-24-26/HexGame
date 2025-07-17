package de.hexgame.nn.training;

import de.hexgame.logic.Game;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Player;
import de.hexgame.nn.CNNPlayer;
import de.hexgame.nn.Model;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Trainer implements Runnable {
    private static final int GAMES_PER_ITERATION = 100;
    private static final int EXPERIENCE_BUFFER_SIZE = 50_000;
    private static final int BATCHES_PER_ITERATION = 250;
    private static final File MODEL_FILE = new File("model.zip");
    private static final File EXPERIENCE_FILE = new File("experience.bin");

    private final ExperienceBuffer experienceBuffer = new ExperienceBuffer(EXPERIENCE_BUFFER_SIZE);
    private final AtomicInteger gameCounter = new AtomicInteger(0);
    private Model model;

    @SneakyThrows
    @Override
    public void run() {
        if (EXPERIENCE_FILE.exists()) {
            experienceBuffer.load(EXPERIENCE_FILE);
        }
        if (MODEL_FILE.exists()) {
            model = new Model(MODEL_FILE, true);
        } else {
            model = new Model();
            if (experienceBuffer.size() == EXPERIENCE_BUFFER_SIZE) {
                fitModel();
                model.save(MODEL_FILE);
            }
        }
        model.start();
        while (true) {
            simulateGames();
            experienceBuffer.save(EXPERIENCE_FILE);
            fitModel();
            model.save(MODEL_FILE);
        }
    }

    @SneakyThrows
    private void simulateGames() {
        log.info("Generating training data...");
        gameCounter.set(0);
        final int threadCount = GAMES_PER_ITERATION;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = Thread.ofVirtual().start(this::simulationTask);
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
    }

    private void fitModel() {
        log.info("Fitting the model...");
        model.fit(experienceBuffer, BATCHES_PER_ITERATION);
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
            gameDataA.extractDataSets(winner == playerA, model, experienceBuffer::add);
            gameDataB.extractDataSets(winner == playerB, model, experienceBuffer::add);
        });

        int gameNumber;
        while ((gameNumber = gameCounter.incrementAndGet()) <= GAMES_PER_ITERATION) {
            game.run();
            gameDataA.clear();
            gameDataB.clear();
            gameState.reset();
            log.info("Game {} has finished.", gameNumber);
        }
    }
}
