package de.hexgame.uifx.gui;

import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import de.hexgame.uifx.networking.HexClient;
import de.hexgame.uifx.networking.HexServer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class HostPane extends VBox {

    public HostPane() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setStyle("-fx-background-color: #1a1a2e;");

        TranslationManager t = TranslationManager.get();

        Label portLabel = new Label(t.translate("port"));
        portLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16;");
        TextField portField = new TextField();
        portField.setPromptText(String.valueOf(HexClient.DEFAULT_PORT));
        portField.setMaxWidth(150);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14;");

        Button startBtn = Styles.styledButton(t.translate("start"));
        startBtn.setOnAction(e -> {
            String portText = portField.getText().trim();
            int port;
            if (portText.isEmpty()) {
                port = HexClient.DEFAULT_PORT;
            } else {
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException ex) {
                    errorLabel.setText(t.translate("port") + ": 1-65535");
                    return;
                }
                if (port < 1 || port > 65535) {
                    errorLabel.setText(t.translate("port") + ": 1-65535");
                    return;
                }
            }
            errorLabel.setText("");
            HexServer server = new HexServer(port);
            NavigationManager.get().navigateTo(new PlayPane(true, server));
        });

        Button backBtn = Styles.styledButton(t.translate("main_menu"));
        backBtn.setOnAction(e -> NavigationManager.get().navigateTo(new MainMenuPane()));

        getChildren().addAll(portLabel, portField, errorLabel, startBtn, backBtn);
    }
}
