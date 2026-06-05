package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.models.Reservation;
import com.lmcode.horecamanager.models.RestaurantTable;
import com.lmcode.horecamanager.repositories.ReservationRepository;
import com.lmcode.horecamanager.repositories.TableRepository;
import com.lmcode.horecamanager.services.ReservationService;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.DateUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ReservationsController extends ControllerSupport {
    private static final List<String> STATUSES = List.of("EN_ATTENTE", "CONFIRMEE", "ARRIVEE", "ANNULEE", "ABSENT");
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final ReservationService reservationService = new ReservationService();
    private final TableRepository tableRepository = new TableRepository();
    private final ObservableList<Reservation> rows = FXCollections.observableArrayList();
    private final TableView<Reservation> tableView = new TableView<>(rows);
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField search = new TextField();

    public ReservationsController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        configureTable();
        search.setPromptText("Client, telephone, statut");
        String pendingSearch = SessionManager.consumePendingSearch();
        if (!pendingSearch.isBlank()) {
            search.setText(pendingSearch);
            datePicker.setValue(null);
        }
        search.textProperty().addListener((observable, oldValue, newValue) -> reload());
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> reload());
        Button filter = ghostButton("Filtrer", "fas-search");
        filter.setOnAction(event -> reload());
        Button all = ghostButton("Tout afficher", "fas-list");
        all.setOnAction(event -> {
            datePicker.setValue(null);
            search.clear();
            reload();
        });
        Button add = primaryButton("Nouvelle reservation", "fas-plus");
        add.setOnAction(event -> editReservation(null));
        Button edit = ghostButton("Modifier", "fas-pen");
        edit.setOnAction(event -> editSelected());
        Button confirm = ghostButton("Confirmer", "fas-check");
        confirm.setOnAction(event -> updateStatus("CONFIRMEE"));
        Button arrived = ghostButton("Arrivee", "fas-user-check");
        arrived.setOnAction(event -> updateStatus("ARRIVEE"));
        Button cancel = ghostButton("Annuler", "fas-ban");
        cancel.setOnAction(event -> updateStatus("ANNULEE"));
        Button delete = ghostButton("Supprimer", "fas-trash");
        delete.setOnAction(event -> deleteSelected());
        HBox tools = toolbar(datePicker, search, filter, all, add, edit, confirm, arrived, cancel, delete);
        reload();
        VBox content = page(tools, reservationStats(), section("Reservations", tableView));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private void configureTable() {
        if (!tableView.getColumns().isEmpty()) {
            return;
        }
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.getColumns().addAll(
                column("Client", Reservation::customerName, 170),
                column("Telephone", reservation -> safe(reservation.phone()), 140),
                column("Date", reservation -> DateUtils.formatDate(reservation.reservationDate()), 120),
                column("Heure", reservation -> DateUtils.formatTime(reservation.reservationTime()), 90),
                column("Pers.", reservation -> String.valueOf(reservation.guestsCount()), 70),
                column("Table", reservation -> reservation.tableId() == null ? "" : "#" + reservation.tableId(), 80),
                statusColumn(),
                column("Note", reservation -> safe(reservation.note()), 230)
        );
        tableView.setPrefHeight(520);
    }

    private TableColumn<Reservation, String> statusColumn() {
        TableColumn<Reservation, String> column = column("Statut", Reservation::status, 130);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : new StatusBadge(item));
            }
        });
        return column;
    }

    private Node reservationStats() {
        long confirmed = rows.stream().filter(row -> "CONFIRMEE".equals(row.status())).count();
        long waiting = rows.stream().filter(row -> "EN_ATTENTE".equals(row.status())).count();
        long late = rows.stream()
                .filter(row -> row.reservationDate().equals(LocalDate.now()))
                .filter(row -> row.reservationTime().isBefore(LocalTime.now()))
                .filter(row -> List.of("EN_ATTENTE", "CONFIRMEE").contains(row.status()))
                .count();
        HBox stats = toolbar(
                new StatusBadge("CONFIRMEES " + confirmed),
                new StatusBadge("EN ATTENTE " + waiting),
                new StatusBadge("EN RETARD " + late)
        );
        return stats;
    }

    private void reload() {
        rows.setAll(reservationRepository.search(datePicker.getValue(), search.getText()));
    }

    private void editSelected() {
        Reservation selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selectionnez une reservation.");
            return;
        }
        editReservation(selected);
    }

    private void updateStatus(String status) {
        Reservation selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selectionnez une reservation.");
            return;
        }
        try {
            switch (status) {
                case "CONFIRMEE" -> reservationService.confirm(selected.id());
                case "ARRIVEE" -> reservationService.arrived(selected.id());
                case "ANNULEE" -> reservationService.cancel(selected.id());
                default -> reservationRepository.updateStatus(selected.id(), status);
            }
            reload();
            showInfo("Reservation mise a jour.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void deleteSelected() {
        Reservation selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selectionnez une reservation.");
            return;
        }
        if (!confirm("Supprimer la reservation", "Supprimer la reservation de " + selected.customerName() + " ?")) {
            return;
        }
        try {
            reservationRepository.delete(selected.id());
            reload();
            showInfo("Reservation supprimee.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void editReservation(Reservation reservation) {
        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle(reservation == null ? "Nouvelle reservation" : "Modifier reservation");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField customer = new TextField(reservation == null ? "" : reservation.customerName());
        TextField phone = new TextField(reservation == null ? "" : safe(reservation.phone()));
        TextField email = new TextField(reservation == null ? "" : safe(reservation.email()));
        DatePicker date = new DatePicker(reservation == null ? LocalDate.now() : reservation.reservationDate());
        TextField time = new TextField(reservation == null ? "19:30" : DateUtils.formatTime(reservation.reservationTime()));
        TextField guests = new TextField(reservation == null ? "2" : String.valueOf(reservation.guestsCount()));
        ComboBox<RestaurantTable> table = new ComboBox<>(FXCollections.observableArrayList(tableRepository.findAll()));
        if (reservation != null && reservation.tableId() != null) {
            tableRepository.findAll().stream().filter(item -> item.id() == reservation.tableId()).findFirst().ifPresent(table::setValue);
        }
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(STATUSES));
        status.setValue(reservation == null ? "EN_ATTENTE" : reservation.status());
        TextArea note = new TextArea(reservation == null ? "" : safe(reservation.note()));
        note.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Client"), customer);
        form.addRow(1, new Label("Telephone"), phone);
        form.addRow(2, new Label("Email"), email);
        form.addRow(3, new Label("Date"), date);
        form.addRow(4, new Label("Heure"), time);
        form.addRow(5, new Label("Personnes"), guests);
        form.addRow(6, new Label("Table"), table);
        form.addRow(7, new Label("Statut"), status);
        form.addRow(8, new Label("Note"), note);
        GridPane.setHgrow(customer, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            RestaurantTable selectedTable = table.getValue();
            return new Reservation(
                    reservation == null ? 0 : reservation.id(),
                    customer.getText().trim(),
                    phone.getText().trim(),
                    email.getText().trim(),
                    date.getValue(),
                    LocalTime.parse(time.getText().trim()),
                    Integer.parseInt(guests.getText().trim()),
                    selectedTable == null ? null : selectedTable.id(),
                    status.getValue(),
                    note.getText().trim(),
                    reservation == null ? LocalDateTime.now() : reservation.createdAt()
            );
        });
        try {
            dialog.showAndWait().ifPresent(value -> {
                if (reservation == null) {
                    reservationService.save(value);
                    showInfo("Reservation creee.");
                } else {
                    reservationService.update(value);
                    showInfo("Reservation modifiee.");
                }
                reload();
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
