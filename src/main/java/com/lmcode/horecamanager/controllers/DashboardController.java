package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.models.Order;
import com.lmcode.horecamanager.models.Reservation;
import com.lmcode.horecamanager.models.RestaurantTable;
import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.PaymentRepository;
import com.lmcode.horecamanager.repositories.ReservationRepository;
import com.lmcode.horecamanager.repositories.TableRepository;
import com.lmcode.horecamanager.repositories.TicketRepository;
import com.lmcode.horecamanager.services.CashRegisterService;
import com.lmcode.horecamanager.ui.components.StatCard;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.DateUtils;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Map;

public class DashboardController extends ControllerSupport {
    private final OrderRepository orderRepository = new OrderRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final TableRepository tableRepository = new TableRepository();
    private final TicketRepository ticketRepository = new TicketRepository();
    private final CashRegisterService cashRegisterService = new CashRegisterService();

    public DashboardController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        GridPane stats = new GridPane();
        stats.getStyleClass().add("stats-grid");
        stats.setHgap(14);
        stats.setVgap(14);
        addStat(stats, 0, 0, "CA du jour", MoneyUtils.format(paymentRepository.totalToday()), "Paiements enregistres", "fas-euro-sign", "cash");
        addStat(stats, 1, 0, "Commandes", String.valueOf(orderRepository.countOpen()), "En cours ou attente", "fas-concierge-bell", "orders");
        addStat(stats, 2, 0, "Tables occupees", String.valueOf(tableRepository.countByStatus("OCCUPEE")), "Salle active", "fas-chair", "tables");
        addStat(stats, 3, 0, "Reservations", String.valueOf(reservationRepository.findToday().size()), "Aujourd'hui", "fas-calendar-check", "reservations");
        addStat(stats, 0, 1, "Tickets", String.valueOf(ticketRepository.countToday()), "Emis aujourd'hui", "fas-receipt", "tickets");
        addStat(stats, 1, 1, "Panier moyen", MoneyUtils.format(orderRepository.averageBasketToday()), "Commandes payees", "fas-chart-line", "reports");
        addStat(stats, 2, 1, "Caisse", cashRegisterService.isOpen() ? "Ouverte" : "Fermee", "Session actuelle", "fas-cash-register", "cash");
        addStat(stats, 3, 1, "Paiements", String.valueOf(paymentRepository.countToday()), "Transactions jour", "fas-credit-card", "cash");

        HBox middle = new HBox(16);
        middle.getChildren().addAll(section("Commandes en attente", orderList()), section("Prochaines reservations", reservationList()));
        middle.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        HBox bottom = new HBox(16);
        bottom.getChildren().addAll(section("Etat des tables", tableState()), section("Produits les plus vendus", topProducts()));
        bottom.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        VBox shortcuts = quickActions();
        ScrollPane scrollPane = new ScrollPane(page(stats, middle, bottom, section("Raccourcis rapides", shortcuts)));
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private void addStat(GridPane grid, int col, int row, String title, String value, String subtitle, String icon, String route) {
        StatCard card = new StatCard(title, value, subtitle, icon);
        card.getStyleClass().add("clickable-card");
        card.setOnMouseClicked(event -> layout.router().navigate(route));
        card.setMaxWidth(Double.MAX_VALUE);
        grid.add(card, col, row);
        GridPane.setHgrow(card, Priority.ALWAYS);
    }

    private Node orderList() {
        VBox list = new VBox(8);
        List<Order> orders = orderRepository.findOpenOrders().stream().limit(5).toList();
        if (orders.isEmpty()) {
            list.getChildren().add(emptyLabel("Aucune commande en attente."));
            return list;
        }
        for (Order order : orders) {
            HBox row = dataRow("Commande #" + order.id(), order.status(), MoneyUtils.format(order.total()));
            row.setOnMouseClicked(event -> layout.router().navigate("orders"));
            list.getChildren().add(row);
        }
        return list;
    }

    private Node reservationList() {
        VBox list = new VBox(8);
        List<Reservation> reservations = reservationRepository.findToday().stream().limit(5).toList();
        if (reservations.isEmpty()) {
            list.getChildren().add(emptyLabel("Aucune reservation aujourd'hui."));
            return list;
        }
        for (Reservation reservation : reservations) {
            HBox row = dataRow(reservation.customerName(), DateUtils.formatTime(reservation.reservationTime()), reservation.guestsCount() + " pers.");
            row.setOnMouseClicked(event -> layout.router().navigate("reservations"));
            list.getChildren().add(row);
        }
        return list;
    }

    private Node tableState() {
        FlowPane flow = new FlowPane(10, 10);
        for (RestaurantTable table : tableRepository.findAll()) {
            VBox item = new VBox(6);
            item.getStyleClass().add("table-state-card");
            item.getStyleClass().add("table-status-" + table.status().toLowerCase().replace('_', '-'));
            Label number = new Label("Table " + table.number());
            number.getStyleClass().add("item-title");
            Label zone = new Label(table.capacity() + " couverts - " + table.zone());
            zone.getStyleClass().add("muted");
            item.getChildren().addAll(number, zone, new StatusBadge(table.status()));
            item.setOnMouseClicked(event -> layout.router().navigate("tables"));
            flow.getChildren().add(item);
        }
        return flow;
    }

    private Node topProducts() {
        VBox list = new VBox(8);
        Map<String, Double> top = orderRepository.topProducts();
        if (top.isEmpty()) {
            list.getChildren().add(emptyLabel("Aucune vente produit."));
            return list;
        }
        top.forEach((product, quantity) -> {
            HBox row = dataRow(product, "Quantite", String.valueOf(quantity.intValue()));
            row.setOnMouseClicked(event -> layout.router().navigate("products"));
            list.getChildren().add(row);
        });
        return list;
    }

    private VBox quickActions() {
        VBox wrap = new VBox(10);
        HBox row = new HBox(10);
        Button order = primaryButton("Nouvelle commande", "fas-plus");
        order.setOnAction(event -> layout.router().navigate("orders"));
        Button reservation = ghostButton("Nouvelle reservation", "fas-calendar-plus");
        reservation.setOnAction(event -> layout.router().navigate("reservations"));
        Button cash = ghostButton("Ouvrir caisse", "fas-cash-register");
        cash.setOnAction(event -> layout.router().navigate("cash"));
        Button ticket = ghostButton("Dernier ticket", "fas-receipt");
        ticket.setOnAction(event -> layout.router().navigate("tickets"));
        Button product = ghostButton("Ajouter produit", "fas-box-open");
        product.setOnAction(event -> layout.router().navigate("products"));
        row.getChildren().addAll(order, reservation, cash, ticket, product);
        wrap.getChildren().add(row);
        return wrap;
    }

    private HBox dataRow(String left, String center, String right) {
        HBox row = new HBox(10);
        row.getStyleClass().add("data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        Label leftLabel = new Label(left);
        leftLabel.getStyleClass().add("item-title");
        Label centerLabel = new Label(center);
        centerLabel.getStyleClass().add("muted");
        Label rightLabel = new Label(right);
        rightLabel.getStyleClass().add("strong");
        HBox.setHgrow(centerLabel, Priority.ALWAYS);
        row.getChildren().addAll(new FontIcon("fas-circle"), leftLabel, centerLabel, rightLabel);
        return row;
    }
}
