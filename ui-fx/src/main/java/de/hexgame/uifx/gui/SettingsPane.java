package de.hexgame.uifx.gui;

import de.hexgame.uifx.ConfigManager;
import de.hexgame.uifx.NavigationManager;
import de.hexgame.uifx.TranslationManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsPane extends VBox {

    private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("en_us", "English");
        LANGUAGES.put("de_de", "Deutsch");
    }

    public SettingsPane() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setStyle("-fx-background-color: #1a1a2e;");

        TranslationManager t = TranslationManager.get();

        Label langLabel = new Label(t.translate("lang"));
        langLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16;");

        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll(LANGUAGES.keySet());
        langCombo.setValue(ConfigManager.get().getLang());
        langCombo.setMaxWidth(200);
        langCombo.setCellFactory(lv -> createLangCell());
        langCombo.setButtonCell(createLangCell());
        langCombo.setStyle("-fx-background-color: #16213e; -fx-text-fill: #00ff88; -fx-font-size: 14;");

        Label savedLabel = new Label();
        savedLabel.setStyle("-fx-text-fill: #44ff44; -fx-font-size: 14;");

        langCombo.setOnAction(e -> {
            String lang = langCombo.getValue();
            ConfigManager.get().setLang(lang);
            ConfigManager.get().save();
            TranslationManager.get().load(lang);
            savedLabel.setText(TranslationManager.get().translate("saved"));
        });

        Button backBtn = Styles.styledButton(t.translate("main_menu"));
        backBtn.setOnAction(e -> NavigationManager.get().navigateTo(new MainMenuPane()));

        getChildren().addAll(langLabel, langCombo, savedLabel, backBtn);
    }

    private ListCell<String> createLangCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(LANGUAGES.getOrDefault(item, item));
                }
                setStyle("-fx-text-fill: #00ff88; -fx-background-color: #16213e;");
            }
        };
    }
}
