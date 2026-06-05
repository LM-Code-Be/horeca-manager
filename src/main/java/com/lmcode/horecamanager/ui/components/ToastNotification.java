package com.lmcode.horecamanager.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class ToastNotification {
    private ToastNotification() {
    }

    public static void show(StackPane host, String message, boolean error) {
        Label toast = new Label(message);
        toast.getStyleClass().add(error ? "toast-error" : "toast");
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        host.getChildren().add(toast);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.2));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeIn.setOnFinished(event -> pause.play());
        pause.setOnFinished(event -> fadeOut.play());
        fadeOut.setOnFinished(event -> host.getChildren().remove(toast));
        fadeIn.play();
    }
}
