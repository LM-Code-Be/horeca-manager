package com.lmcode.horecamanager.ui.layout;

import com.lmcode.horecamanager.app.AppConfig;
import com.lmcode.horecamanager.app.Router;
import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.controllers.CalculatorController;
import com.lmcode.horecamanager.controllers.CashRegisterController;
import com.lmcode.horecamanager.controllers.DashboardController;
import com.lmcode.horecamanager.controllers.MenuController;
import com.lmcode.horecamanager.controllers.OrdersController;
import com.lmcode.horecamanager.controllers.ProductsController;
import com.lmcode.horecamanager.controllers.ReportsController;
import com.lmcode.horecamanager.controllers.ReservationsController;
import com.lmcode.horecamanager.controllers.SettingsController;
import com.lmcode.horecamanager.controllers.TablesController;
import com.lmcode.horecamanager.controllers.TicketsController;
import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.ProductRepository;
import com.lmcode.horecamanager.repositories.ReservationRepository;
import com.lmcode.horecamanager.repositories.TableRepository;
import com.lmcode.horecamanager.repositories.TicketRepository;
import com.lmcode.horecamanager.ui.components.SidebarButton;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.components.ToastNotification;
import com.lmcode.horecamanager.utils.DateUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.net.URI;

public class MainLayout extends StackPane {
    private final StackPane contentPane = new StackPane();
    private final Label pageTitle = new Label("Dashboard");
    private final StatusBadge cashBadge = new StatusBadge("FERMEE");
    private final Router router = new Router(contentPane, pageTitle::setText);
    private final ProductRepository productRepository = new ProductRepository();
    private final TicketRepository ticketRepository = new TicketRepository();
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final TableRepository tableRepository = new TableRepository();
    private final OrderRepository orderRepository = new OrderRepository();

    public MainLayout() {
        getStyleClass().add("app-root");
        BorderPane shell = new BorderPane();
        shell.setLeft(createSidebar());
        shell.setTop(createTopbar());
        shell.setCenter(contentPane);
        shell.setBottom(createFooter());
        getChildren().add(shell);
        registerRoutes();
        SessionManager.cashOpenProperty().addListener((observable, oldValue, open) -> updateCashBadge());
        updateCashBadge();
        Platform.runLater(() -> router.navigate("dashboard"));
    }

    public Router router() {
        return router;
    }

    public void showToast(String message) {
        ToastNotification.show(this, message, false);
    }

    public void showError(String message) {
        ToastNotification.show(this, message, true);
    }

    public void refreshCashStatus() {
        SessionManager.refreshCashStatus();
        updateCashBadge();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(18));
        ImageView logo = new ImageView(new Image(SessionManager.resource("/icons/logo.jpg"), 44, 44, true, true));
        logo.getStyleClass().add("brand-logo");
        Label brand = new Label(AppConfig.APP_NAME);
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("Restaurant POS");
        subtitle.getStyleClass().add("brand-subtitle");
        VBox brandTexts = new VBox(2, brand, subtitle);
        HBox brandBox = new HBox(12, logo, brandTexts);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.getStyleClass().add("brand-box");
        sidebar.getChildren().addAll(brandBox, spacer(12));
        addRouteButton(sidebar, "dashboard", "Dashboard", "fas-tachometer-alt");
        addRouteButton(sidebar, "tables", "Tables", "fas-chair");
        addRouteButton(sidebar, "reservations", "Reservations", "fas-calendar-check");
        addRouteButton(sidebar, "orders", "Commandes", "fas-concierge-bell");
        addRouteButton(sidebar, "cash", "Caisse", "fas-cash-register");
        addRouteButton(sidebar, "tickets", "Tickets", "fas-receipt");
        addRouteButton(sidebar, "menu", "Menu", "fas-utensils");
        addRouteButton(sidebar, "products", "Produits", "fas-box-open");
        addRouteButton(sidebar, "calculator", "Calculatrice", "fas-calculator");
        addRouteButton(sidebar, "reports", "Rapports", "fas-chart-line");
        addRouteButton(sidebar, "settings", "Parametres", "fas-cog");
        return sidebar;
    }

    private HBox createTopbar() {
        HBox topbar = new HBox(14);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(14, 20, 14, 20));
        pageTitle.getStyleClass().add("page-title");
        TextField search = new TextField();
        search.setPromptText("Recherche globale");
        search.getStyleClass().add("search-field");
        search.setOnAction(event -> runGlobalSearch(search.getText()));
        HBox.setHgrow(search, Priority.ALWAYS);
        Label date = new Label(DateUtils.todayLabel());
        date.getStyleClass().add("muted");
        Button theme = new Button("", new FontIcon("fas-adjust"));
        theme.getStyleClass().add("icon-button");
        theme.setOnAction(event -> SessionManager.toggleTheme(getScene()));
        Button newOrder = new Button("Nouvelle commande", new FontIcon("fas-plus"));
        newOrder.getStyleClass().add("primary-button");
        newOrder.setOnAction(event -> router.navigate("orders"));
        topbar.getChildren().addAll(pageTitle, search, date, cashBadge, theme, newOrder);
        return topbar;
    }

    private void runGlobalSearch(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isBlank()) {
            showToast("Saisissez une recherche.");
            return;
        }
        SessionManager.setPendingSearch(value);
        String normalized = value.toLowerCase();
        if (tableRepository.findAll().stream().anyMatch(table ->
                table.number().equalsIgnoreCase(value)
                        || contains(table.name(), normalized)
                        || contains(table.zone(), normalized)
                        || contains(table.status(), normalized))) {
            router.navigate("tables");
            return;
        }
        if (productRepository.findAll().stream().anyMatch(product ->
                contains(product.name(), normalized) || contains(product.description(), normalized))) {
            router.navigate("products");
            return;
        }
        if (ticketRepository.findAll().stream().anyMatch(ticket ->
                contains(ticket.ticketNumber(), normalized) || String.valueOf(ticket.orderId()).equals(value))) {
            router.navigate("tickets");
            return;
        }
        if (reservationRepository.findAll().stream().anyMatch(reservation ->
                contains(reservation.customerName(), normalized) || contains(reservation.phone(), normalized))) {
            router.navigate("reservations");
            return;
        }
        if (orderRepository.findOpenOrders().stream().anyMatch(order -> String.valueOf(order.id()).equals(value))) {
            router.navigate("orders");
            return;
        }
        router.navigate("products");
        showToast("Recherche appliquee aux produits.");
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private HBox createFooter() {
        HBox footer = new HBox(6);
        footer.getStyleClass().add("app-footer");
        footer.setAlignment(Pos.CENTER);
        Label left = new Label("Copyright 2026 - Developpeur Michael de");
        left.getStyleClass().add("footer-text");
        Hyperlink link = new Hyperlink("LM-Code");
        link.getStyleClass().add("footer-link");
        link.setOnAction(event -> openLmCode());
        Label right = new Label("- lm-code.be");
        right.getStyleClass().add("footer-text");
        footer.getChildren().addAll(left, link, right);
        return footer;
    }

    private void openLmCode() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create("https://lm-code.be"));
            } else {
                showToast("https://lm-code.be");
            }
        } catch (Exception exception) {
            showError("Impossible d'ouvrir lm-code.be");
        }
    }

    private void registerRoutes() {
        router.register("dashboard", "Dashboard", () -> new DashboardController(this).view(), findButton("dashboard"));
        router.register("tables", "Tables", () -> new TablesController(this).view(), findButton("tables"));
        router.register("reservations", "Reservations", () -> new ReservationsController(this).view(), findButton("reservations"));
        router.register("orders", "Commandes", () -> new OrdersController(this).view(), findButton("orders"));
        router.register("cash", "Caisse", () -> new CashRegisterController(this).view(), findButton("cash"));
        router.register("tickets", "Tickets", () -> new TicketsController(this).view(), findButton("tickets"));
        router.register("menu", "Menu", () -> new MenuController(this).view(), findButton("menu"));
        router.register("products", "Produits", () -> new ProductsController(this).view(), findButton("products"));
        router.register("calculator", "Calculatrice", () -> new CalculatorController(this).view(), findButton("calculator"));
        router.register("reports", "Rapports", () -> new ReportsController(this).view(), findButton("reports"));
        router.register("settings", "Parametres", () -> new SettingsController(this).view(), findButton("settings"));
    }

    private void addRouteButton(VBox sidebar, String route, String label, String icon) {
        SidebarButton button = new SidebarButton(label, icon);
        button.setId("nav-" + route);
        sidebar.getChildren().add(button);
    }

    private SidebarButton findButton(String route) {
        VBox sidebar = (VBox) ((BorderPane) getChildren().get(0)).getLeft();
        return sidebar.getChildren()
                .stream()
                .filter(SidebarButton.class::isInstance)
                .map(SidebarButton.class::cast)
                .filter(button -> ("nav-" + route).equals(button.getId()))
                .findFirst()
                .orElse(null);
    }

    private void updateCashBadge() {
        cashBadge.setStatus(SessionManager.isCashOpen() ? "OUVERTE" : "FERMEE");
    }

    private Label spacer(int height) {
        Label spacer = new Label();
        spacer.setMinHeight(height);
        return spacer;
    }
}
