package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.models.CashSession;
import com.lmcode.horecamanager.models.Payment;
import com.lmcode.horecamanager.repositories.PaymentRepository;
import com.lmcode.horecamanager.services.CalculatorService;
import com.lmcode.horecamanager.services.CashRegisterService;
import com.lmcode.horecamanager.ui.components.StatCard;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.DateUtils;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

public class CashRegisterController extends ControllerSupport {
    private final CashRegisterService cashRegisterService = new CashRegisterService();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final CalculatorService calculatorService = new CalculatorService();
    private final ObservableList<Payment> rows = FXCollections.observableArrayList();
    private final TableView<Payment> tableView = new TableView<>(rows);
    private final VBox summary = new VBox(14);

    public CashRegisterController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        configureTable();
        Button open = primaryButton("Ouvrir caisse", "fas-lock-open");
        open.setOnAction(event -> openDialog());
        Button close = ghostButton("Fermer caisse", "fas-lock");
        close.setOnAction(event -> closeDialog());
        Button refresh = ghostButton("Actualiser", "fas-sync");
        refresh.setOnAction(event -> reload());
        HBox tools = toolbar(open, close, refresh);
        HBox sections = new HBox(16, section("Session", summary), section("Calcul rendu monnaie", changeCalculator()));
        sections.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        VBox content = page(tools, sections, section("Dernieres transactions", tableView));
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
                column("Commande", payment -> "#" + payment.orderId(), 100),
                column("Moyen", Payment::method, 160),
                column("Montant", payment -> MoneyUtils.format(payment.amount()), 120),
                column("Recu", payment -> payment.amountReceived() == null ? "" : MoneyUtils.format(payment.amountReceived()), 120),
                column("Rendu", payment -> payment.changeDue() == null ? "" : MoneyUtils.format(payment.changeDue()), 120),
                column("Date", payment -> DateUtils.formatDateTime(payment.paidAt()), 180)
        );
        tableView.setPlaceholder(emptyLabel("Aucune transaction. Les paiements apparaitront ici apres encaissement."));
        tableView.setPrefHeight(340);
    }

    private void reload() {
        rows.setAll(paymentRepository.findRecent(30));
        summary.getChildren().clear();
        CashSession session = cashRegisterService.currentSession();
        if (session == null) {
            summary.getChildren().addAll(new StatusBadge("FERMEE"), emptyLabel("Aucune session de caisse."));
        } else {
            summary.getChildren().addAll(
                    new StatusBadge(session.status()),
                    new StatCard("Montant initial", MoneyUtils.format(session.initialAmount()), "Ouverture: " + DateUtils.formatDateTime(session.openedAt()), "fas-wallet"),
                    new StatCard("Montant theorique", MoneyUtils.format(session.expectedAmount()), "Caisse actuelle", "fas-cash-register"),
                    new StatCard("Total encaisse", MoneyUtils.format(cashRegisterService.totalToday()), cashRegisterService.paymentCountToday() + " paiements", "fas-credit-card")
            );
            if (session.realAmount() != null) {
                summary.getChildren().add(new StatCard("Ecart", MoneyUtils.format(session.differenceAmount()), "Cloture: " + DateUtils.formatDateTime(session.closedAt()), "fas-balance-scale"));
            }
        }
        VBox methods = new VBox(6);
        for (Map.Entry<String, Double> entry : cashRegisterService.totalsByMethodToday().entrySet()) {
            methods.getChildren().add(new Label(entry.getKey() + "  " + MoneyUtils.format(entry.getValue())));
        }
        summary.getChildren().add(section("Paiements du jour", methods));
    }

    private Node changeCalculator() {
        TextField total = new TextField("0");
        TextField received = new TextField("0");
        Label result = new Label(MoneyUtils.format(0));
        result.getStyleClass().add("stat-value");
        Button calculate = primaryButton("Calculer", "fas-calculator");
        calculate.setOnAction(event -> {
            try {
                double change = calculatorService.changeDue(MoneyUtils.parseAmount(total.getText()), MoneyUtils.parseAmount(received.getText()));
                result.setText(MoneyUtils.format(change));
            } catch (RuntimeException exception) {
                showError(exception);
            }
        });
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Total a payer"), total);
        form.addRow(1, new Label("Montant recu"), received);
        form.addRow(2, new Label("Rendu"), result);
        form.addRow(3, new Label(), calculate);
        return form;
    }

    private void openDialog() {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ouvrir caisse");
        ButtonType open = new ButtonType("Ouvrir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(open, ButtonType.CANCEL);
        TextField initial = new TextField("100");
        TextArea note = new TextArea();
        note.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Montant initial"), initial);
        form.addRow(1, new Label("Commentaire"), note);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == open ? MoneyUtils.parseAmount(initial.getText()) : null);
        try {
            dialog.showAndWait().ifPresent(amount -> {
                cashRegisterService.open(amount, note.getText());
                layout.refreshCashStatus();
                reload();
                showInfo("Caisse ouverte.");
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void closeDialog() {
        CashSession session = cashRegisterService.currentSession();
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Fermer caisse");
        ButtonType close = new ButtonType("Fermer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(close, ButtonType.CANCEL);
        TextField real = new TextField(session == null ? "0" : String.valueOf(MoneyUtils.round(session.expectedAmount())));
        TextArea note = new TextArea();
        note.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Montant reel"), real);
        form.addRow(1, new Label("Commentaire"), note);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == close ? MoneyUtils.parseAmount(real.getText()) : null);
        try {
            dialog.showAndWait().ifPresent(amount -> {
                cashRegisterService.close(amount, note.getText());
                layout.refreshCashStatus();
                reload();
                showInfo("Caisse fermee.");
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }
}
