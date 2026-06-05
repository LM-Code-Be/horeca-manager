package com.lmcode.horecamanager;

import com.lmcode.horecamanager.app.AppConfig;
import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.database.DatabaseInitializer;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        installExceptionHandler();
        DatabaseInitializer.initialize();
        SessionManager.initialize();
        MainLayout layout = new MainLayout();
        Scene scene = new Scene(layout, 1360, 840);
        scene.getStylesheets().add(SessionManager.resource("/styles/app.css"));
        SessionManager.applyTheme(scene);
        stage.setTitle(AppConfig.APP_NAME);
        stage.getIcons().add(new Image(SessionManager.resource("/icons/logo.jpg")));
        stage.setMinWidth(AppConfig.MIN_WIDTH);
        stage.setMinHeight(AppConfig.MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void installExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof IllegalArgumentException) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(AppConfig.APP_NAME);
                    alert.setHeaderText("Action impossible");
                    alert.setContentText(throwable.getMessage());
                    alert.showAndWait();
                });
                return;
            }
            throwable.printStackTrace();
        });
    }
}
