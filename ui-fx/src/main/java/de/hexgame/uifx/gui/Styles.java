package de.hexgame.uifx.gui;

import javafx.scene.control.Button;

public class Styles {
    public static final String BTN_NORMAL = "-fx-background-color: #16213e; -fx-text-fill: #00ff88; -fx-font-size: 16; -fx-cursor: hand; -fx-background-radius: 5;";
    public static final String BTN_HOVER = "-fx-background-color: #0f3460; -fx-text-fill: #00ff88; -fx-font-size: 16; -fx-cursor: hand; -fx-background-radius: 5;";

    public static void applyButtonStyle(Button btn) {
        btn.setStyle(BTN_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(BTN_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(BTN_NORMAL));
    }

    public static Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(40);
        applyButtonStyle(btn);
        return btn;
    }

    public static Button styledButton(String text, double width) {
        Button btn = new Button(text);
        btn.setPrefWidth(width);
        btn.setPrefHeight(40);
        applyButtonStyle(btn);
        return btn;
    }
}
