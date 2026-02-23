package de.hexgame.uifx.gui;

import de.hexgame.logic.Player;
import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import de.hexgame.uifx.board.HexBoardController;
import de.hexgame.uifx.board.UIPlayer;
import de.hexgame.uifx.networking.HexServer;
import de.hexgame.uifx.networking.RemotePlayer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
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

public class PlayPane extends VBox {
    private static final int DIFFICULTIES = 6;
    private final List<Class<? extends Player>> playerClasses = new ArrayList<>();
    private int firstSelection = -1;
    private int secondSelection = -1;
    private int difficultyFirst = 0;
    private int difficultySecond = 0;
    private final boolean withRemote;
    private final HexServer server;
    private final List<Button> firstButtons = new ArrayList<>();
    private final List<Button> secondButtons = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public PlayPane(boolean withRemote, HexServer server) {
        this.withRemote = withRemote;
        this.server = server;

        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #1a1a2e;");

        TranslationManager t = TranslationManager.get();

        // Load player classes
        if (withRemote) playerClasses.add(RemotePlayer.class);
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("playerClasses.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
            for (String line : reader.lines().toList()) {
                try {
                    Class<?> clazz = Class.forName(line.trim());
                    playerClasses.add((Class<? extends Player>) clazz);
                } catch (ClassNotFoundException ignored) {
                } catch (ClassCastException e) {
                    throw new RuntimeException("Player class must implement Player: " + line, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Label title = new Label(t.translate("play"));
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

        // Start button
        Button startBtn = Styles.styledButton(t.translate("start"));
        startBtn.setOnAction(e -> startGame());

        // Back button
        Button backBtn = Styles.styledButton(t.translate("main_menu"));
        backBtn.setOnAction(e -> NavigationManager.get().navigateTo(new MainMenuPane()));

        getChildren().addAll(title, columns, startBtn, backBtn);
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

    private void startGame() {
        if (firstSelection < 0 || secondSelection < 0) return;
        try {
            Player first = instantiatePlayer(firstSelection, difficultyFirst, 0);
            Player second = instantiatePlayer(secondSelection, difficultySecond, first instanceof RemotePlayer ? 1 : 0);

            HexBoardController controller = HexBoardController.get();
            controller.createBoardPane();
            controller.startGame(first, second, withRemote, server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Player instantiatePlayer(int classIdx, int difficulty, int remoteIdx) throws Exception {
        Class<? extends Player> clazz = playerClasses.get(classIdx);
        if (withRemote && clazz == RemotePlayer.class) {
            RemotePlayer rp = server.getPlayerList().get(remoteIdx);
            server.addRelevantPlayer(rp);
            return rp;
        }
        // Try constructor with int (difficulty)
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
