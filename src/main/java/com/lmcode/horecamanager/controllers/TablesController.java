package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.models.RestaurantTable;
import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.TableRepository;
import com.lmcode.horecamanager.services.OrderService;
import com.lmcode.horecamanager.services.ValidationService;
import com.lmcode.horecamanager.ui.components.TableCard;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class TablesController extends ControllerSupport {
    private static final List<String> STATUSES = List.of("LIBRE", "OCCUPEE", "RESERVEE", "A_NETTOYER", "INDISPONIBLE");
    private final TableRepository tableRepository = new TableRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final OrderService orderService = new OrderService();
    private final ValidationService validationService = new ValidationService();
    private final ObservableList<RestaurantTable> rows = FXCollections.observableArrayList();
    private final FlowPane cards = new FlowPane(14, 14);
    private final TableView<RestaurantTable> tableView = new TableView<>(rows);
    private final TextField search = new TextField();

    public TablesController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        cards.getStyleClass().add("cards-flow");
        configureTable();
        search.setPromptText("Table, zone, statut");
        String pendingSearch = SessionManager.consumePendingSearch();
        if (!pendingSearch.isBlank()) {
            search.setText(pendingSearch);
        }
        search.textProperty().addListener((observable, oldValue, newValue) -> reload());
        Button add = primaryButton("Ajouter table", "fas-plus");
        add.setOnAction(event -> editTable(null));
        Button edit = ghostButton("Modifier", "fas-pen");
        edit.setOnAction(event -> {
            RestaurantTable table = tableView.getSelectionModel().getSelectedItem();
            if (table == null) {
                showInfo("Selectionnez une table.");
                return;
            }
            editTable(table);
        });
        Button delete = ghostButton("Supprimer", "fas-trash");
        delete.setOnAction(event -> deleteSelected());
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(STATUSES));
        status.setPromptText("Changer statut");
        Button apply = ghostButton("Appliquer", "fas-check");
        apply.setOnAction(event -> {
            RestaurantTable selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null || status.getValue() == null) {
                showInfo("Selection et statut requis.");
                return;
            }
            if (orderRepository.findOpenByTableId(selected.id()) != null && !"OCCUPEE".equals(status.getValue())) {
                showInfo("Table liee a une commande active. Encaissez ou annulez la commande avant de changer ce statut.");
                return;
            }
            tableRepository.updateStatus(selected.id(), status.getValue());
            reload();
            showInfo("Statut modifie.");
        });
        HBox tools = toolbar(search, add, edit, delete, status, apply);
        VBox content = page(tools, section("Plan de salle", cards), section("Liste detaillee", tableView));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        reload();
        return scrollPane;
    }

    private void configureTable() {
        if (!tableView.getColumns().isEmpty()) {
            return;
        }
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.getColumns().addAll(
                column("Numero", RestaurantTable::number, 90),
                column("Nom", table -> safe(table.name()), 180),
                column("Capacite", table -> String.valueOf(table.capacity()), 90),
                column("Zone", table -> safe(table.zone()), 130),
                column("Statut", RestaurantTable::status, 130),
                column("Commentaire", table -> safe(table.note()), 220)
        );
        tableView.setPrefHeight(280);
    }

    private void reload() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        rows.setAll(tableRepository.findAll().stream()
                .filter(table -> query.isBlank()
                        || contains(table.number(), query)
                        || contains(table.name(), query)
                        || contains(table.zone(), query)
                        || contains(table.status(), query)
                        || contains(table.note(), query))
                .toList());
        cards.getChildren().clear();
        for (RestaurantTable table : rows) {
            var activeOrder = orderRepository.findOpenByTableId(table.id());
            String activeInfo = activeOrder == null ? "" : "Commande #" + activeOrder.id() + " - " + MoneyUtils.format(activeOrder.total());
            TableCard card = new TableCard(table, activeInfo, this::createOrderForTable, this::freeTable);
            card.setPrefWidth(250);
            card.setOnMouseClicked(event -> tableView.getSelectionModel().select(table));
            cards.getChildren().add(card);
        }
    }

    private void createOrderForTable(RestaurantTable table) {
        try {
            int orderId = orderService.createOrder(table.id(), "SUR_PLACE");
            reload();
            showInfo("Commande #" + orderId + " creee.");
            layout.router().navigate("orders");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void freeTable(RestaurantTable table) {
        try {
            if (orderRepository.findOpenByTableId(table.id()) != null) {
                showInfo("Table liee a une commande active. Encaissez ou annulez la commande avant de liberer.");
                return;
            }
            tableRepository.updateStatus(table.id(), "LIBRE");
            reload();
            showInfo("Table liberee.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void deleteSelected() {
        RestaurantTable selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selectionnez une table.");
            return;
        }
        if (!confirm("Supprimer la table", "Supprimer " + selected.displayName() + " ?")) {
            return;
        }
        try {
            if (orderRepository.findOpenByTableId(selected.id()) != null) {
                showInfo("Table liee a une commande active. Suppression impossible.");
                return;
            }
            tableRepository.delete(selected.id());
            reload();
            showInfo("Table supprimee.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void editTable(RestaurantTable table) {
        Dialog<RestaurantTable> dialog = new Dialog<>();
        dialog.setTitle(table == null ? "Nouvelle table" : "Modifier table");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField number = new TextField(table == null ? "" : table.number());
        TextField name = new TextField(table == null ? "" : safe(table.name()));
        TextField capacity = new TextField(table == null ? "4" : String.valueOf(table.capacity()));
        ComboBox<String> zone = new ComboBox<>(FXCollections.observableArrayList("Salle", "Terrasse", "Bar", "Etage"));
        zone.setValue(table == null ? "Salle" : safe(table.zone(), "Salle"));
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(STATUSES));
        status.setValue(table == null ? "LIBRE" : table.status());
        TextArea note = new TextArea(table == null ? "" : safe(table.note()));
        note.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Numero"), number);
        form.addRow(1, new Label("Nom"), name);
        form.addRow(2, new Label("Capacite"), capacity);
        form.addRow(3, new Label("Zone"), zone);
        form.addRow(4, new Label("Statut"), status);
        form.addRow(5, new Label("Commentaire"), note);
        GridPane.setHgrow(number, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            int parsedCapacity = Integer.parseInt(capacity.getText().trim());
            validationService.validateTable(number.getText(), parsedCapacity);
            return new RestaurantTable(
                    table == null ? 0 : table.id(),
                    number.getText().trim(),
                    name.getText().trim(),
                    parsedCapacity,
                    zone.getValue(),
                    status.getValue(),
                    note.getText().trim()
            );
        });
        try {
            dialog.showAndWait().ifPresent(value -> {
                if (table == null) {
                    tableRepository.save(value);
                    showInfo("Table ajoutee.");
                } else {
                    if (orderRepository.findOpenByTableId(table.id()) != null && !"OCCUPEE".equals(value.status())) {
                        showInfo("Table liee a une commande active. Gardez le statut OCCUPEE jusqu'au paiement ou a l'annulation.");
                        return;
                    }
                    tableRepository.update(value);
                    showInfo("Table modifiee.");
                }
                reload();
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private String safe(String value) {
        return safe(value, "");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
