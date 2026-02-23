package de.hexgame.uifx.gui;

import de.hexgame.logic.Game;
import de.hexgame.logic.GameState;
import de.hexgame.logic.Player;
import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import de.hexgame.uifx.board.UIPlayer;
import de.hexgame.uifx.networking.RemotePlayer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationPane extends VBox {
    private static final int DIFFICULTIES = 6;
    private final List<Class<? extends Player>> playerClasses = new ArrayList<>();
    private int firstSelection = -1;
    private int secondSelection = -1;
    private int difficultyFirst = 0;
    private int difficultySecond = 0;
    private final List<Button> firstButtons = new ArrayList<>();
    private final List<Button> secondButtons = new ArrayList<>();
    private final TextField gameCountField;
    private final Label progressLabel;
    private final Label resultsLabel;
    private final Button startBtn;
    private final Button cancelBtn;
    private ExecutorService executor;

    @SuppressWarnings("unchecked")
    public SimulationPane() {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #1a1a2e;");

        TranslationManager t = TranslationManager.get();

        // Load player classes, filtering out UIPlayer and RemotePlayer
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("playerClasses.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
            for (String line : reader.lines().toList()) {
                try {
                    Class<?> clazz = Class.forName(line.trim());
                    if (UIPlayer.class.isAssignableFrom(clazz) || RemotePlayer.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    playerClasses.add((Class<? extends Player>) clazz);
                } catch (ClassNotFoundException ignored) {
                } catch (ClassCastException e) {
                    throw new RuntimeException("Player class must implement Player: " + line, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Label title = new Label(t.translate("simulate"));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24;");

        // Player selection columns
        Label firstLabel = new Label(t.translate("first"));
        firstLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 16;");
        Label secondLabel = new Label(t.translate("second"));
        secondLabel.setStyle("-fx-text-fill: #4488ff; -fx-font-size: 16;");

        VBox firstCol = new VBox(8);
        firstCol.setAlignment(Pos.CENTER);
        firstCol.getChildren().add(firstLabel);

        VBox secondCol = new VBox(8);
        secondCol.setAlignment(Pos.CENTER);
        secondCol.getChildren().add(secondLabel);

        for (int i = 0; i < playerClasses.size(); i++) {
            final int idx = i;
            String name = t.translate(playerClasses.get(i).getName());

            Button fb = playerButton(name);
            fb.setOnAction(e -> selectFirst(idx));
            firstCol.getChildren().add(fb);
            firstButtons.add(fb);

            Button sb = playerButton(name);
            sb.setOnAction(e -> selectSecond(idx));
            secondCol.getChildren().add(sb);
            secondButtons.add(sb);
        }

        // Difficulty sliders
        Label diffLabel1 = new Label(t.translate("difficulty"));
        diffLabel1.setStyle("-fx-text-fill: white;");
        Slider diffSlider1 = new Slider(0, DIFFICULTIES - 1, 0);
        diffSlider1.setBlockIncrement(1);
        diffSlider1.setMajorTickUnit(1);
        diffSlider1.setMinorTickCount(0);
        diffSlider1.setSnapToTicks(true);
        diffSlider1.setShowTickLabels(true);
        diffSlider1.setMaxWidth(150);
        diffSlider1.valueProperty().addListener((obs, o, n) -> difficultyFirst = n.intValue());
        firstCol.getChildren().addAll(diffLabel1, diffSlider1);

        Label diffLabel2 = new Label(t.translate("difficulty"));
        diffLabel2.setStyle("-fx-text-fill: white;");
        Slider diffSlider2 = new Slider(0, DIFFICULTIES - 1, 0);
        diffSlider2.setBlockIncrement(1);
        diffSlider2.setMajorTickUnit(1);
        diffSlider2.setMinorTickCount(0);
        diffSlider2.setSnapToTicks(true);
        diffSlider2.setShowTickLabels(true);
        diffSlider2.setMaxWidth(150);
        diffSlider2.valueProperty().addListener((obs, o, n) -> difficultySecond = n.intValue());
        secondCol.getChildren().addAll(diffLabel2, diffSlider2);

        HBox columns = new HBox(40, firstCol, secondCol);
        columns.setAlignment(Pos.CENTER);

        // Game count input
        Label gameCountLabel = new Label(t.translate("num_games"));
        gameCountLabel.setStyle("-fx-text-fill: white;");
        gameCountField = new TextField("100");
        gameCountField.setMaxWidth(100);
        gameCountField.setStyle("-fx-background-color: #16213e; -fx-text-fill: white; -fx-border-color: #0f3460; -fx-border-radius: 5; -fx-background-radius: 5;");
        HBox gameCountRow = new HBox(10, gameCountLabel, gameCountField);
        gameCountRow.setAlignment(Pos.CENTER);

        // Progress and results
        progressLabel = new Label();
        progressLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 14;");
        resultsLabel = new Label();
        resultsLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 16;");

        // Buttons
        startBtn = Styles.styledButton(t.translate("start"));
        startBtn.setOnAction(e -> startSimulation());

        cancelBtn = Styles.styledButton(t.translate("cancel"));
        cancelBtn.setOnAction(e -> cancelSimulation());
        cancelBtn.setVisible(false);

        Button backBtn = Styles.styledButton(t.translate("main_menu"));
        backBtn.setOnAction(e -> {
            cancelSimulation();
            NavigationManager.get().navigateTo(new MainMenuPane());
        });

        getChildren().addAll(title, columns, gameCountRow, startBtn, cancelBtn, progressLabel, resultsLabel, backBtn);
    }

    private void selectFirst(int idx) {
        if (firstSelection >= 0) resetButton(firstButtons.get(firstSelection));
        firstSelection = idx;
        highlightButton(firstButtons.get(idx), "#ff4444");
    }

    private void selectSecond(int idx) {
        if (secondSelection >= 0) resetButton(secondButtons.get(secondSelection));
        secondSelection = idx;
        highlightButton(secondButtons.get(idx), "#4488ff");
    }

    private void startSimulation() {
        if (firstSelection < 0 || secondSelection < 0) return;

        int numGames;
        try {
            numGames = Integer.parseInt(gameCountField.getText().trim());
            if (numGames <= 0) return;
        } catch (NumberFormatException e) {
            return;
        }

        TranslationManager t = TranslationManager.get();
        String player1Name = t.translate(playerClasses.get(firstSelection).getName());
        String player2Name = t.translate(playerClasses.get(secondSelection).getName());

        startBtn.setDisable(true);
        cancelBtn.setVisible(true);
        resultsLabel.setText("");
        progressLabel.setText(t.translate("progress") + ": 0 / " + numGames);

        AtomicInteger player1Wins = new AtomicInteger(0);
        AtomicInteger player2Wins = new AtomicInteger(0);
        AtomicInteger gamesCompleted = new AtomicInteger(0);

        int threads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < numGames; i++) {
            executor.submit(() -> {
                try {
                    Player first = instantiatePlayer(firstSelection, difficultyFirst);
                    Player second = instantiatePlayer(secondSelection, difficultySecond);
                    Game game = new Game(new GameState(), first, second);

                    game.addPlayerWinListener(winner -> {
                        if (winner == first) {
                            player1Wins.incrementAndGet();
                        } else {
                            player2Wins.incrementAndGet();
                        }
                    });

                    game.run();

                    int completed = gamesCompleted.incrementAndGet();
                    Platform.runLater(() -> {
                        progressLabel.setText(t.translate("progress") + ": " + completed + " / " + numGames);
                        if (completed == numGames) {
                            showResults(player1Name, player1Wins.get(), player2Name, player2Wins.get(), numGames);
                        }
                    });
                } catch (Exception e) {
                    gamesCompleted.incrementAndGet();
                }
            });
        }

        executor.shutdown();
    }

    private void showResults(String p1Name, int p1Wins, String p2Name, int p2Wins, int total) {
        TranslationManager t = TranslationManager.get();
        double p1Pct = total > 0 ? (p1Wins * 100.0 / total) : 0;
        double p2Pct = total > 0 ? (p2Wins * 100.0 / total) : 0;

        resultsLabel.setText(String.format(
                "%s:\n%s: %d %s (%.1f%%)\n%s: %d %s (%.1f%%)",
                t.translate("results"),
                p1Name, p1Wins, t.translate("wins"), p1Pct,
                p2Name, p2Wins, t.translate("wins"), p2Pct
        ));

        startBtn.setDisable(false);
        cancelBtn.setVisible(false);
    }

    private void cancelSimulation() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            startBtn.setDisable(false);
            cancelBtn.setVisible(false);
            progressLabel.setText(progressLabel.getText() + " (" + TranslationManager.get().translate("cancel") + ")");
        }
    }

    private Player instantiatePlayer(int classIdx, int difficulty) throws Exception {
        Class<? extends Player> clazz = playerClasses.get(classIdx);
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterCount() == 1 && c.getParameterTypes()[0] == int.class) {
                return clazz.getConstructor(int.class).newInstance(difficulty);
            }
        }
        return clazz.getConstructor().newInstance();
    }

    private Button playerButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(160);
        btn.setPrefHeight(40);
        btn.setStyle(Styles.BTN_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(Styles.BTN_HOVER));
        btn.setOnMouseExited(e -> {
            int idx = firstButtons.indexOf(btn);
            if (idx >= 0 && idx == firstSelection) {
                highlightButton(btn, "#ff4444");
                return;
            }
            idx = secondButtons.indexOf(btn);
            if (idx >= 0 && idx == secondSelection) {
                highlightButton(btn, "#4488ff");
                return;
            }
            btn.setStyle(Styles.BTN_NORMAL);
        });
        return btn;
    }

    private void highlightButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: #16213e; -fx-text-fill: " + color + "; -fx-font-size: 16; -fx-cursor: hand; -fx-background-radius: 5; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 5;");
    }

    private void resetButton(Button btn) {
        btn.setStyle(Styles.BTN_NORMAL);
    }
}
