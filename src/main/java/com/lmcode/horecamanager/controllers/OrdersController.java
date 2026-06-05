package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.models.Category;
import com.lmcode.horecamanager.models.Order;
import com.lmcode.horecamanager.models.OrderItem;
import com.lmcode.horecamanager.models.Product;
import com.lmcode.horecamanager.models.RestaurantTable;
import com.lmcode.horecamanager.models.Ticket;
import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.repositories.CategoryRepository;
import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.ProductRepository;
import com.lmcode.horecamanager.repositories.TableRepository;
import com.lmcode.horecamanager.services.CashRegisterService;
import com.lmcode.horecamanager.services.OrderService;
import com.lmcode.horecamanager.ui.components.ProductCard;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

public class OrdersController extends ControllerSupport {
    private static final List<String> ORDER_TYPES = List.of("SUR_PLACE", "A_EMPORTER", "LIVRAISON");
    private static final List<String> PAYMENT_METHODS = List.of("ESPECES", "CARTE", "TICKET_RESTAURANT", "VIREMENT", "AUTRE");
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final TableRepository tableRepository = new TableRepository();
    private final OrderService orderService = new OrderService();
    private final CashRegisterService cashRegisterService = new CashRegisterService();
    private final ObservableList<Order> openOrders = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();
    private final ComboBox<Order> orderCombo = new ComboBox<>(openOrders);
    private final TableView<OrderItem> cart = new TableView<>(cartItems);
    private final FlowPane categoryPane = new FlowPane(8, 8);
    private final FlowPane productPane = new FlowPane(12, 12);
    private final TextField productSearch = new TextField();
    private final Label subtotal = new Label();
    private final Label vat = new Label();
    private final Label discount = new Label();
    private final Label total = new Label();
    private Button payButton;
    private Label orderContext;
    private Integer selectedCategoryId;

    public OrdersController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        configureOrderCombo();
        configureCart();
        Button newOrder = primaryButton("Nouvelle commande", "fas-plus");
        newOrder.setOnAction(event -> createOrderDialog());
        Button waiting = ghostButton("Mettre en attente", "fas-pause");
        waiting.setOnAction(event -> markWaiting());
        Button discountButton = ghostButton("Remise", "fas-percent");
        discountButton.setOnAction(event -> discountDialog());
        Button cancel = ghostButton("Annuler", "fas-ban");
        cancel.setOnAction(event -> cancelOrder());
        Button refresh = ghostButton("Actualiser", "fas-sync");
        refresh.setOnAction(event -> reloadOrders());
        orderContext = new Label();
        orderContext.getStyleClass().add("order-context");
        HBox top = toolbar(orderCombo, newOrder, waiting, discountButton, cancel, refresh, orderContext);
        orderCombo.setPrefWidth(260);

        VBox categories = new VBox(12, new Label("Categories"), categoryPane);
        categories.getStyleClass().add("pos-side");
        categories.setPrefWidth(190);
        productSearch.setPromptText("Rechercher produit");
        productSearch.textProperty().addListener((observable, oldValue, newValue) -> reloadProducts(selectedCategoryId));
        ScrollPane productScroll = new ScrollPane(productPane);
        productScroll.setFitToWidth(true);
        productScroll.getStyleClass().add("product-scroll");
        productScroll.viewportBoundsProperty().addListener((observable, oldValue, bounds) -> productPane.setPrefWrapLength(Math.max(520, bounds.getWidth() - 20)));
        VBox products = new VBox(12, new Label("Produits"), productSearch, productScroll);
        VBox.setVgrow(productScroll, Priority.ALWAYS);
        products.getStyleClass().add("pos-products");
        VBox basket = createBasket();

        HBox pos = new HBox(16, categories, products, basket);
        HBox.setHgrow(products, Priority.ALWAYS);
        BorderPane border = new BorderPane();
        border.setTop(new VBox(10, top, cashBanner()));
        border.setCenter(pos);
        BorderPane.setMargin(pos, new Insets(16, 0, 0, 0));
        VBox page = page(border);
        String pendingSearch = SessionManager.consumePendingSearch();
        reloadOrders();
        if (!pendingSearch.isBlank() && !selectOrder(pendingSearch)) {
            productSearch.setText(pendingSearch);
        }
        reloadCategories();
        reloadProducts(null);
        return page;
    }

    private void configureOrderCombo() {
        orderCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Order order) {
                if (order == null) {
                    return "";
                }
                String table = order.tableId() == null ? "Sans table" : "Table " + order.tableId();
                return "#" + order.id() + " - " + table + " - " + MoneyUtils.format(order.total());
            }

            @Override
            public Order fromString(String string) {
                return null;
            }
        });
        orderCombo.valueProperty().addListener((observable, oldValue, newValue) -> reloadCart());
    }

    private void configureCart() {
        if (!cart.getColumns().isEmpty()) {
            return;
        }
        cart.getStyleClass().add("data-table");
        cart.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cart.getColumns().addAll(
                column("Produit", OrderItem::productName, 150),
                column("Qt", item -> String.valueOf(item.quantity()), 60),
                column("Prix", item -> MoneyUtils.format(item.unitPrice()), 80),
                column("Total", item -> MoneyUtils.format(item.total()), 90)
        );
        cart.setPlaceholder(emptyLabel("Ajoutez un produit depuis la carte."));
        cart.setPrefHeight(360);
    }

    private VBox createBasket() {
        Button minus = ghostButton("-", "fas-minus");
        minus.setOnAction(event -> changeQuantity(-1));
        Button plus = ghostButton("+", "fas-plus");
        plus.setOnAction(event -> changeQuantity(1));
        Button remove = ghostButton("Retirer", "fas-trash");
        remove.setOnAction(event -> setQuantity(0));
        HBox quantityTools = new HBox(8, minus, plus, remove);
        quantityTools.setAlignment(Pos.CENTER_LEFT);
        VBox totals = new VBox(8,
                totalRow("Sous-total HT", subtotal),
                totalRow("TVA", vat),
                totalRow("Remise", discount),
                totalRow("Total TTC", total)
        );
        totals.getStyleClass().add("totals-box");
        payButton = primaryButton("Paiement", "fas-credit-card");
        payButton.setMaxWidth(Double.MAX_VALUE);
        payButton.setOnAction(event -> paymentDialog());
        VBox basket = new VBox(12, new Label("Panier"), cart, quantityTools, totals, payButton);
        basket.getStyleClass().add("pos-basket");
        basket.setPrefWidth(390);
        return basket;
    }

    private HBox cashBanner() {
        HBox banner = new HBox(12);
        banner.getStyleClass().add("cash-warning");
        banner.setAlignment(Pos.CENTER_LEFT);
        StatusBadge status = new StatusBadge(cashRegisterService.isOpen() ? "CAISSE OUVERTE" : "CAISSE FERMEE");
        Label text = new Label(cashRegisterService.isOpen()
                ? "Les commandes peuvent etre encaissees."
                : "Ouvrez la caisse avant un paiement. Le bouton paiement proposera aussi une ouverture rapide.");
        text.getStyleClass().add("muted");
        HBox.setHgrow(text, Priority.ALWAYS);
        Button open = ghostButton("Ouvrir caisse", "fas-lock-open");
        open.setOnAction(event -> openCashBeforePayment());
        open.setVisible(!cashRegisterService.isOpen());
        open.setManaged(!cashRegisterService.isOpen());
        banner.getChildren().addAll(status, text, open);
        return banner;
    }

    private HBox totalRow(String label, Label value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label left = new Label(label);
        left.getStyleClass().add("muted");
        value.getStyleClass().add("strong");
        HBox.setHgrow(left, Priority.ALWAYS);
        row.getChildren().addAll(left, value);
        return row;
    }

    private void reloadOrders() {
        Order previous = orderCombo.getValue();
        openOrders.setAll(orderRepository.findOpenOrders());
        if (previous != null && openOrders.stream().noneMatch(order -> order.id() == previous.id())) {
            orderCombo.setValue(null);
        }
        if (!openOrders.isEmpty() && orderCombo.getValue() == null) {
            orderCombo.setValue(openOrders.get(0));
        }
        reloadCart();
    }

    private void reloadCategories() {
        categoryPane.getChildren().clear();
        Button all = ghostButton("Tous", "fas-layer-group");
        all.setOnAction(event -> reloadProducts(null));
        categoryPane.getChildren().add(all);
        for (Category category : categoryRepository.findActive()) {
            Button button = ghostButton(category.name(), "fas-tag");
            button.setOnAction(event -> reloadProducts(category.id()));
            categoryPane.getChildren().add(button);
        }
    }

    private void reloadProducts(Integer categoryId) {
        selectedCategoryId = categoryId;
        productPane.getChildren().clear();
        List<Product> products = categoryId == null ? productRepository.findAvailable() : productRepository.findByCategory(categoryId);
        String query = productSearch.getText() == null ? "" : productSearch.getText().trim().toLowerCase();
        for (Product product : products) {
            if (!query.isBlank()
                    && !contains(product.name(), query)
                    && !contains(product.description(), query)
                    && !contains(statusLabel(product), query)) {
                continue;
            }
            ProductCard card = new ProductCard(product, this::addProduct);
            card.setPrefWidth(240);
            card.setMinHeight(140);
            productPane.getChildren().add(card);
        }
        if (productPane.getChildren().isEmpty()) {
            productPane.getChildren().add(emptyLabel("Aucun produit disponible pour ce filtre."));
        }
    }

    private void reloadCart() {
        Order order = orderCombo.getValue();
        if (order == null) {
            cartItems.clear();
            subtotal.setText(MoneyUtils.format(0));
            vat.setText(MoneyUtils.format(0));
            discount.setText(MoneyUtils.format(0));
            total.setText(MoneyUtils.format(0));
            orderContext.setText("Aucune commande active.");
            if (payButton != null) {
                payButton.setDisable(true);
            }
            return;
        }
        Order refreshed = orderRepository.findById(order.id());
        cartItems.setAll(orderRepository.findItems(order.id()));
        subtotal.setText(MoneyUtils.format(refreshed.subtotal()));
        vat.setText(MoneyUtils.format(refreshed.vatAmount()));
        discount.setText(MoneyUtils.format(refreshed.discount()));
        total.setText(MoneyUtils.format(refreshed.total()));
        String table = refreshed.tableId() == null ? "Sans table" : "Table " + refreshed.tableId();
        orderContext.setText(table + " - " + refreshed.orderType() + " - " + cartItems.size() + " ligne(s)");
        if (payButton != null) {
            payButton.setDisable(cartItems.isEmpty());
        }
        orderCombo.setValue(refreshed);
    }

    private void addProduct(Product product) {
        Order order = orderCombo.getValue();
        if (order == null) {
            showInfo("Creez une commande.");
            return;
        }
        try {
            orderService.addProduct(order.id(), product, 1);
            reloadOrders();
            openOrders.stream().filter(item -> item.id() == order.id()).findFirst().ifPresent(orderCombo::setValue);
            showInfo(stockMessage(product.id(), order.id()));
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void changeQuantity(int delta) {
        OrderItem selected = cart.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selectionnez une ligne.");
            return;
        }
        setQuantity(selected.quantity() + delta);
    }

    private void setQuantity(int quantity) {
        OrderItem selected = cart.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selectionnez une ligne.");
            return;
        }
        try {
            Order order = orderCombo.getValue();
            orderService.updateItemQuantity(selected.id(), quantity);
            reloadOrders();
            openOrders.stream().filter(item -> item.id() == order.id()).findFirst().ifPresent(orderCombo::setValue);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void createOrderDialog() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle commande");
        ButtonType create = new ButtonType("Creer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
        ComboBox<RestaurantTable> table = new ComboBox<>(FXCollections.observableArrayList(tableRepository.findAll()));
        table.setPromptText("Sans table");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(ORDER_TYPES));
        type.setValue("SUR_PLACE");
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Table"), table);
        form.addRow(1, new Label("Type"), type);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != create) {
                return null;
            }
            RestaurantTable selected = table.getValue();
            return orderService.createOrder(selected == null ? null : selected.id(), type.getValue());
        });
        try {
            dialog.showAndWait().ifPresent(orderId -> {
                reloadOrders();
                openOrders.stream().filter(item -> item.id() == orderId).findFirst().ifPresent(orderCombo::setValue);
                showInfo("Commande creee.");
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void discountDialog() {
        Order order = orderCombo.getValue();
        if (order == null) {
            showInfo("Selectionnez une commande.");
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Remise");
        ButtonType save = new ButtonType("Appliquer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField amount = new TextField(String.valueOf(order.discount()));
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Montant"), amount);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == save ? MoneyUtils.parseAmount(amount.getText()) : null);
        try {
            dialog.showAndWait().ifPresent(value -> {
                orderService.applyDiscount(order.id(), value);
                reloadOrders();
                openOrders.stream().filter(item -> item.id() == order.id()).findFirst().ifPresent(orderCombo::setValue);
                showInfo("Remise appliquee.");
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void markWaiting() {
        Order order = orderCombo.getValue();
        if (order == null) {
            showInfo("Selectionnez une commande.");
            return;
        }
        orderRepository.markWaiting(order.id());
        reloadOrders();
        showInfo("Commande mise en attente.");
    }

    private void cancelOrder() {
        Order order = orderCombo.getValue();
        if (order == null) {
            showInfo("Selectionnez une commande.");
            return;
        }
        if (!confirm("Annuler la commande", "Annuler la commande #" + order.id() + " ?")) {
            return;
        }
        orderService.cancelOrder(order.id());
        reloadOrders();
        showInfo("Commande annulee.");
    }

    private void paymentDialog() {
        Order order = orderCombo.getValue();
        if (order == null) {
            showInfo("Selectionnez une commande.");
            return;
        }
        if (!cashRegisterService.isOpen() && !openCashBeforePayment()) {
            return;
        }
        Order refreshed = orderRepository.findById(order.id());
        Dialog<Ticket> dialog = new Dialog<>();
        dialog.setTitle("Paiement commande #" + order.id());
        ButtonType pay = new ButtonType("Encaisser", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(pay, ButtonType.CANCEL);
        ComboBox<String> method = new ComboBox<>(FXCollections.observableArrayList(PAYMENT_METHODS));
        method.setValue("ESPECES");
        TextField received = new TextField(String.valueOf(MoneyUtils.round(refreshed.total())));
        Label due = new Label(MoneyUtils.format(refreshed.total()));
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Total"), due);
        form.addRow(1, new Label("Moyen"), method);
        form.addRow(2, new Label("Montant recu"), received);
        dialog.getDialogPane().setContent(form);
        final Ticket[] paidTicket = new Ticket[1];
        dialog.getDialogPane().lookupButton(pay).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                paidTicket[0] = orderService.payOrder(order.id(), method.getValue(), MoneyUtils.parseAmount(received.getText()));
            } catch (RuntimeException exception) {
                event.consume();
                showError(exception);
            }
        });
        dialog.setResultConverter(button -> button == pay ? paidTicket[0] : null);
        try {
            dialog.showAndWait().ifPresent(ticket -> {
                layout.refreshCashStatus();
                reloadOrders();
                reloadProducts(null);
                showInfo("Paiement enregistre. Ticket genere.");
                layout.router().navigate("tickets");
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private boolean openCashBeforePayment() {
        if (cashRegisterService.isOpen()) {
            return true;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ouvrir caisse");
        ButtonType open = new ButtonType("Ouvrir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(open, ButtonType.CANCEL);
        TextField initial = new TextField("100");
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Fond de caisse"), initial);
        form.addRow(1, new Label("Statut"), new Label("La caisse doit etre ouverte avant d'encaisser."));
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == open ? MoneyUtils.parseAmount(initial.getText()) : null);
        try {
            return dialog.showAndWait().map(amount -> {
                cashRegisterService.open(amount, "Ouverture depuis l'encaissement");
                layout.refreshCashStatus();
                showInfo("Caisse ouverte.");
                return true;
            }).orElse(false);
        } catch (RuntimeException exception) {
            showError(exception);
            return false;
        }
    }

    private boolean selectOrder(String query) {
        String value = query == null ? "" : query.trim().replace("#", "");
        if (value.isBlank()) {
            return false;
        }
        return openOrders.stream()
                .filter(order -> String.valueOf(order.id()).equals(value))
                .findFirst()
                .map(order -> {
                    orderCombo.setValue(order);
                    return true;
                })
                .orElse(false);
    }

    private String stockMessage(int productId, int orderId) {
        Product product = productRepository.findById(productId);
        if (product == null || product.stockQuantity() == null) {
            return "Produit ajoute.";
        }
        int inOrder = orderRepository.findItems(orderId)
                .stream()
                .filter(item -> item.productId() == productId)
                .mapToInt(OrderItem::quantity)
                .sum();
        int reservedElsewhere = orderRepository.reservedQuantityForProduct(productId, orderId);
        int remaining = product.stockQuantity() - reservedElsewhere - inOrder;
        if (remaining <= 0) {
            return "Produit ajoute. Stock entierement reserve dans cette commande.";
        }
        if (remaining <= ProductRepository.DEFAULT_LOW_STOCK_THRESHOLD) {
            return "Produit ajoute. Stock faible: " + remaining + " unite(s) restantes.";
        }
        return "Produit ajoute.";
    }

    private String statusLabel(Product product) {
        if (product.stockQuantity() != null && product.stockQuantity() <= 0) {
            return "RUPTURE";
        }
        if (product.stockQuantity() != null && product.stockQuantity() <= ProductRepository.DEFAULT_LOW_STOCK_THRESHOLD) {
            return "STOCK FAIBLE";
        }
        return product.available() ? "DISPONIBLE" : "INDISPONIBLE";
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
