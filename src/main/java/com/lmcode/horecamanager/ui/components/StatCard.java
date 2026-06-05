package com.lmcode.horecamanager.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class StatCard extends VBox {
    private final Label valueLabel = new Label();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();

    public StatCard(String title, String value, String subtitle, String iconLiteral) {
        getStyleClass().add("stat-card");
        setSpacing(10);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("stat-icon");
        titleLabel.setText(title);
        titleLabel.getStyleClass().add("card-title");
        header.getChildren().addAll(icon, titleLabel);
        valueLabel.setText(value);
        valueLabel.getStyleClass().add("stat-value");
        subtitleLabel.setText(subtitle);
        subtitleLabel.getStyleClass().add("muted");
        getChildren().addAll(header, valueLabel, subtitleLabel);
    }

    public void update(String value, String subtitle) {
        valueLabel.setText(value);
        subtitleLabel.setText(subtitle);
    }
}
