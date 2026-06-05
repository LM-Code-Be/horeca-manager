package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.ui.layout.MainLayout;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;
import java.util.function.Function;

abstract class ControllerSupport {
    protected final MainLayout layout;

    protected ControllerSupport(MainLayout layout) {
        this.layout = layout;
    }

    protected VBox page(Node... children) {
        VBox page = new VBox(18);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(22));
        page.getChildren().addAll(children);
        return page;
    }

    protected VBox section(String title, Node content) {
        VBox section = new VBox(12);
        section.getStyleClass().add("section");
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        section.getChildren().addAll(label, content);
        return section;
    }

    protected HBox toolbar(Node... children) {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        toolbar.getChildren().addAll(children);
        return toolbar;
    }

    protected Button primaryButton(String text, String icon) {
        Button button = new Button(text, new FontIcon(icon));
        button.getStyleClass().add("primary-button");
        return button;
    }

    protected Button ghostButton(String text, String icon) {
        Button button = new Button(text, new FontIcon(icon));
        button.getStyleClass().add("ghost-button");
        return button;
    }

    protected void stretch(Node node) {
        HBox.setHgrow(node, Priority.ALWAYS);
    }

    protected boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    protected void showInfo(String message) {
        layout.showToast(message);
    }

    protected void showError(Throwable throwable) {
        String message = throwable instanceof IllegalArgumentException ? throwable.getMessage() : "Erreur: " + throwable.getMessage();
        layout.showError(message == null ? "Erreur inconnue." : message);
    }

    protected <T> TableColumn<T, String> column(String title, Function<T, String> mapper, int width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(mapper.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    protected Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-label");
        return label;
    }
}
