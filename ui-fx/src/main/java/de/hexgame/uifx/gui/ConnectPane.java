package de.hexgame.uifx.gui;

import de.hexgame.logic.Position;
import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import de.hexgame.uifx.board.HexBoardController;
import de.hexgame.uifx.networking.HexClient;
import de.hexgame.uifx.networking.packets.ClientNetworkCallback;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ConnectPane extends VBox {
    private final Label statusLabel;

    public ConnectPane() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setStyle("-fx-background-color: #1a1a2e;");

        TranslationManager t = TranslationManager.get();

        Label hostLabel = new Label(t.translate("host"));
        hostLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16;");
        TextField hostField = new TextField();
        hostField.setPromptText("localhost:25567");
        hostField.setMaxWidth(250);

        Label nameLabel = new Label(t.translate("name"));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16;");
        TextField nameField = new TextField();
        nameField.setPromptText("Player");
        nameField.setMaxWidth(250);

        Button connectBtn = Styles.styledButton(t.translate("connect"));
        connectBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            if (host.isEmpty()) host = "localhost";
            HexBoardController controller = HexBoardController.get();
            controller.setRemote(true);
            ClientNetworkCallback callback = new ConnectPaneCallback(controller, this);
            HexClient client = new HexClient(host, callback);
            client.sendConnect(nameField.getText().trim());
        });

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #44ff44; -fx-font-size: 14;");

        Button backBtn = Styles.styledButton(t.translate("main_menu"));
        backBtn.setOnAction(e -> {
            HexClient.stop();
            NavigationManager.get().navigateTo(new MainMenuPane());
        });

        getChildren().addAll(hostLabel, hostField, nameLabel, nameField, connectBtn, statusLabel, backBtn);
    }

    public void showConnected() {
        statusLabel.setText(TranslationManager.get().translate("connect_suc"));
    }

    public void showNotConnected() {
        statusLabel.setText(TranslationManager.get().translate("connect_un"));
        statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14;");
    }

    private record ConnectPaneCallback(HexBoardController controller, ConnectPane pane) implements ClientNetworkCallback {
        @Override
        public void onBoardChange(Position pos, int color) {
            controller.onBoardChange(pos, color);
        }

        @Override
        public void onBoardClear() {
            controller.onBoardClear();
        }

        @Override
        public void onGameStart() {
            controller.onGameStart();
        }

        @Override
        public void onConnectionState(boolean connected) {
            Platform.runLater(() -> {
                if (connected) pane.showConnected();
                else pane.showNotConnected();
            });
        }

        @Override
        public void onGameEnd(String playerName) {
            controller.onGameEnd(playerName);
        }
    }
}
