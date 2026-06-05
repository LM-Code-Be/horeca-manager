package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.models.Ticket;
import com.lmcode.horecamanager.repositories.SettingsRepository;
import com.lmcode.horecamanager.repositories.TicketRepository;
import com.lmcode.horecamanager.services.TicketService;
import com.lmcode.horecamanager.ui.components.TicketPreview;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.DateUtils;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;

public class TicketsController extends ControllerSupport {
    private final TicketRepository ticketRepository = new TicketRepository();
    private final TicketService ticketService = new TicketService();
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final ObservableList<Ticket> rows = FXCollections.observableArrayList();
    private final TableView<Ticket> tableView = new TableView<>(rows);
    private final TicketPreview preview = new TicketPreview();
    private final TextField search = new TextField();
    private List<Ticket> allTickets = List.of();

    public TicketsController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        configureTable();
        search.setPromptText("Ticket, commande, total, date");
        String pendingSearch = SessionManager.consumePendingSearch();
        if (!pendingSearch.isBlank()) {
            search.setText(pendingSearch);
        }
        search.textProperty().addListener((observable, oldValue, newValue) -> filter());
        Button filter = ghostButton("Rechercher", "fas-search");
        filter.setOnAction(event -> filter());
        Button clear = ghostButton("Tout", "fas-list");
        clear.setOnAction(event -> {
            search.clear();
            filter();
        });
        Button exportTxt = primaryButton("Exporter TXT", "fas-file-alt");
        exportTxt.setOnAction(event -> exportSelected(false));
        Button exportPdf = ghostButton("Exporter PDF", "fas-file-pdf");
        exportPdf.setOnAction(event -> exportSelected(true));
        Button reprint = ghostButton("Reimprimer", "fas-print");
        reprint.setOnAction(event -> {
            if (selected() == null) {
                showInfo("Selectionnez un ticket.");
            } else {
                showInfo("Ticket envoye a l'impression.");
            }
        });
        HBox tools = toolbar(search, filter, clear, exportTxt, exportPdf, reprint);
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, ticket) -> preview.setText(ticket == null ? "" : ticket.content()));
        HBox body = new HBox(16, section("Historique", tableView), section("Apercu ticket", preview));
        body.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        VBox content = page(tools, body);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        reload();
        filter();
        return scrollPane;
    }

    private void configureTable() {
        if (!tableView.getColumns().isEmpty()) {
            return;
        }
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.getColumns().addAll(
                column("Numero", Ticket::ticketNumber, 190),
                column("Commande", ticket -> "#" + ticket.orderId(), 100),
                column("Total", ticket -> MoneyUtils.format(ticket.total()), 110),
                column("Date", ticket -> DateUtils.formatDateTime(ticket.createdAt()), 170)
        );
        tableView.setPlaceholder(emptyLabel("Aucun ticket. Encaissez une commande pour generer un recu."));
        tableView.setPrefHeight(540);
    }

    private void reload() {
        allTickets = ticketRepository.findAll();
        rows.setAll(allTickets);
        if (!rows.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        } else {
            preview.clear();
        }
    }

    private void filter() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        if (query.isBlank()) {
            rows.setAll(allTickets);
        } else {
            rows.setAll(allTickets.stream()
                    .filter(ticket -> contains(ticket.ticketNumber(), query)
                            || contains("#" + ticket.orderId(), query)
                            || contains(String.valueOf(ticket.orderId()), query)
                            || contains(MoneyUtils.format(ticket.total()), query)
                            || contains(DateUtils.formatDateTime(ticket.createdAt()), query)
                            || contains(ticket.content(), query))
                    .toList());
        }
        if (!rows.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        } else {
            preview.clear();
        }
    }

    private void exportSelected(boolean pdf) {
        Ticket ticket = selected();
        if (ticket == null) {
            showInfo("Selectionnez un ticket.");
            return;
        }
        try {
            String folder = settingsRepository.get("app.exportFolder", "exports");
            Path path = Path.of(folder, ticket.ticketNumber() + (pdf ? ".pdf" : ".txt"));
            if (pdf) {
                ticketService.exportPdf(ticket, path);
            } else {
                ticketService.exportTxt(ticket, path);
            }
            showInfo("Ticket exporte: " + path);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private Ticket selected() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
