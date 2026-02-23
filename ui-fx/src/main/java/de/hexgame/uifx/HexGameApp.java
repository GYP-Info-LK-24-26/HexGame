package de.hexgame.uifx;

import de.hexgame.uifx.board.HexBoardController;
import de.hexgame.uifx.gui.MainMenuPane;
import de.hexgame.uifx.networking.HexClient;
import de.hexgame.uifx.networking.HexServer;
import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class HexGameApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HexGame");
        primaryStage.setWidth(900);
        primaryStage.setHeight(700);
        primaryStage.setResizable(false);

        NavigationManager.get().init(primaryStage);

        // Load config and translations
        ConfigManager config = ConfigManager.get();
        TranslationManager.get().load(config.getLang());

        // ESC key handler
        NavigationManager.get().getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                HexBoardController ctrl = HexBoardController.get();
                if (ctrl.isRunning()) {
                    ctrl.forceEnd();
                    NavigationManager.get().navigateTo(new MainMenuPane());
                }
            }
        });

        NavigationManager.get().navigateTo(new MainMenuPane());
        primaryStage.show();
    }

    @Override
    public void stop() {
        HexBoardController ctrl = HexBoardController.get();
        if (ctrl.isRunning()) {
            ctrl.forceEnd();
        }
        HexClient.stop();
        HexServer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
