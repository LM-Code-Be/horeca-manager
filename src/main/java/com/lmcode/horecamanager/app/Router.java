package com.lmcode.horecamanager.app;

import com.lmcode.horecamanager.ui.components.SidebarButton;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Router {
    private final StackPane contentPane;
    private final Consumer<String> titleConsumer;
    private final Map<String, Route> routes = new LinkedHashMap<>();
    private final Map<String, SidebarButton> buttons = new LinkedHashMap<>();

    public Router(StackPane contentPane, Consumer<String> titleConsumer) {
        this.contentPane = contentPane;
        this.titleConsumer = titleConsumer;
    }

    public void register(String key, String title, Supplier<Node> supplier, SidebarButton button) {
        routes.put(key, new Route(title, supplier));
        if (button != null) {
            buttons.put(key, button);
            button.setOnAction(event -> navigate(key));
        }
    }

    public void navigate(String key) {
        Route route = routes.get(key);
        if (route == null) {
            return;
        }
        Node node = route.supplier().get();
        node.setOpacity(0);
        contentPane.getChildren().setAll(node);
        FadeTransition transition = new FadeTransition(Duration.millis(140), node);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
        titleConsumer.accept(route.title());
        buttons.forEach((routeKey, button) -> button.setActive(routeKey.equals(key)));
    }

    private record Route(String title, Supplier<Node> supplier) {
    }
}
