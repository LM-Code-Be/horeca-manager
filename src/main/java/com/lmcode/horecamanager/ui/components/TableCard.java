package com.lmcode.horecamanager.ui.components;

import com.lmcode.horecamanager.models.RestaurantTable;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.util.Duration;

import java.util.function.Consumer;

public class TableCard extends VBox {
    public TableCard(RestaurantTable table, Consumer<RestaurantTable> openOrderAction, Consumer<RestaurantTable> freeAction) {
        this(table, null, openOrderAction, freeAction);
    }

    public TableCard(RestaurantTable table, String activeOrderInfo, Consumer<RestaurantTable> openOrderAction, Consumer<RestaurantTable> freeAction) {
        getStyleClass().add("table-card");
        getStyleClass().add("table-status-" + normalize(table.status()));
        setSpacing(10);
        Label title = new Label(table.displayName());
        title.getStyleClass().add("item-title");
        Label details = new Label(table.capacity() + " couverts - " + nullSafe(table.zone()));
        details.getStyleClass().add("muted");
        StatusBadge badge = new StatusBadge(table.status());
        Label note = new Label(noteText(table, activeOrderInfo));
        note.getStyleClass().add(activeOrderInfo == null || activeOrderInfo.isBlank() ? "muted" : "order-chip");
        note.setWrapText(true);
        Button open = new Button("Commande", new FontIcon("fas-receipt"));
        open.getStyleClass().add("primary-button");
        open.setOnAction(event -> openOrderAction.accept(table));
        open.setDisable("INDISPONIBLE".equals(table.status()) || "A_NETTOYER".equals(table.status()));
        Button free = new Button("Liberer", new FontIcon("fas-check"));
        free.getStyleClass().add("ghost-button");
        free.setOnAction(event -> freeAction.accept(table));
        free.setDisable("LIBRE".equals(table.status()));
        HBox actions = new HBox(8, open, free);
        actions.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(title, details, badge, note, actions);
        if ("OCCUPEE".equals(table.status())) {
            FadeTransition pulse = new FadeTransition(Duration.seconds(1.2), this);
            pulse.setFromValue(0.88);
            pulse.setToValue(1.0);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(FadeTransition.INDEFINITE);
            pulse.play();
        }
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "Zone" : value;
    }

    private String noteText(RestaurantTable table, String activeOrderInfo) {
        if (activeOrderInfo != null && !activeOrderInfo.isBlank()) {
            return activeOrderInfo;
        }
        return table.note() == null ? "" : table.note();
    }

    private String normalize(String status) {
        return status == null ? "neutral" : status.toLowerCase().replace('_', '-');
    }
}
