package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.services.CalculatorService;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CalculatorController extends ControllerSupport {
    private final CalculatorService calculatorService = new CalculatorService();

    public CalculatorController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        HBox body = new HBox(16, section("Calculatrice", standardCalculator()), section("Mode caisse", cashCalculator()), section("TVA et remise", vatCalculator()));
        body.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        ScrollPane scrollPane = new ScrollPane(page(body));
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private Node standardCalculator() {
        TextField left = new TextField("0");
        TextField right = new TextField("0");
        Label result = resultLabel();
        Button add = operation("+", () -> calculatorService.add(amount(left), amount(right)), result);
        Button subtract = operation("-", () -> calculatorService.subtract(amount(left), amount(right)), result);
        Button multiply = operation("x", () -> calculatorService.multiply(amount(left), amount(right)), result);
        Button divide = operation("/", () -> calculatorService.divide(amount(left), amount(right)), result);
        Button percent = operation("%", () -> calculatorService.percentage(amount(left), amount(right)), result);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Valeur A"), left);
        form.addRow(1, new Label("Valeur B"), right);
        form.addRow(2, new Label("Resultat"), result);
        form.addRow(3, new Label(), new HBox(8, add, subtract, multiply, divide, percent));
        return form;
    }

    private Node cashCalculator() {
        TextField total = new TextField("0");
        TextField received = new TextField("0");
        Label change = resultLabel();
        Button calculate = operation("Calculer", () -> calculatorService.changeDue(amount(total), amount(received)), change);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Total a payer"), total);
        form.addRow(1, new Label("Montant recu"), received);
        form.addRow(2, new Label("Monnaie"), change);
        form.addRow(3, new Label(), calculate);
        return form;
    }

    private Node vatCalculator() {
        TextField amount = new TextField("0");
        TextField rate = new TextField("21");
        Label vat = resultLabel();
        Label discount = resultLabel();
        Button vatButton = operation("TVA", () -> calculatorService.vatAmount(amount(amount), amount(rate)), vat);
        Button discountButton = operation("Remise", () -> calculatorService.discount(amount(amount), amount(rate)), discount);
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Montant TTC"), amount);
        form.addRow(1, new Label("Taux"), rate);
        form.addRow(2, new Label("TVA incluse"), vat);
        form.addRow(3, new Label("Remise"), discount);
        form.addRow(4, new Label(), new HBox(8, vatButton, discountButton));
        return form;
    }

    private Button operation(String text, Operation operation, Label result) {
        Button button = text.length() <= 2 ? ghostButton(text, "fas-calculator") : primaryButton(text, "fas-calculator");
        button.setOnAction(event -> {
            try {
                result.setText(MoneyUtils.format(operation.calculate()));
            } catch (RuntimeException exception) {
                showError(exception);
            }
        });
        return button;
    }

    private Label resultLabel() {
        Label label = new Label(MoneyUtils.format(0));
        label.getStyleClass().add("stat-value");
        return label;
    }

    private double amount(TextField field) {
        return MoneyUtils.parseAmount(field.getText());
    }

    @FunctionalInterface
    private interface Operation {
        double calculate();
    }
}
