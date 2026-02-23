package de.hexgame.uifx.gui;

import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class MainMenuPane extends VBox {
    private final Label messageLabel;

    public MainMenuPane() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setStyle("-fx-background-color: #1a1a2e;");

        TranslationManager t = TranslationManager.get();

        Label title = new Label("HexGame");
        title.setFont(Font.font(36));
        title.setStyle("-fx-text-fill: white;");

        Button playBtn = Styles.styledButton(t.translate("play"));
        Button simulateBtn = Styles.styledButton(t.translate("simulate"));
        Button settingsBtn = Styles.styledButton(t.translate("settings"));
        Button connectBtn = Styles.styledButton(t.translate("connect"));
        Button hostBtn = Styles.styledButton(t.translate("host"));
        Button quitBtn = Styles.styledButton(t.translate("quit"));

        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #44ff44; -fx-font-size: 14;");

        playBtn.setOnAction(e -> NavigationManager.get().navigateTo(new PlayPane(false, null)));
        simulateBtn.setOnAction(e -> NavigationManager.get().navigateTo(new SimulationPane()));
        settingsBtn.setOnAction(e -> NavigationManager.get().navigateTo(new SettingsPane()));
        connectBtn.setOnAction(e -> NavigationManager.get().navigateTo(new ConnectPane()));
        hostBtn.setOnAction(e -> NavigationManager.get().navigateTo(new HostPane()));
        quitBtn.setOnAction(e -> NavigationManager.get().getStage().close());

        getChildren().addAll(title, playBtn, simulateBtn, settingsBtn, connectBtn, hostBtn, quitBtn, messageLabel);
    }

    public void showMessage(String msg) {
        messageLabel.setText(msg);
    }
}
