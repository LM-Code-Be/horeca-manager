package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.models.AppSettings;
import com.lmcode.horecamanager.repositories.SettingsRepository;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsController extends ControllerSupport {
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final TextField name = new TextField();
    private final TextField address = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final TextField vatNumber = new TextField();
    private final TextField currency = new TextField();
    private final TextField vatRate = new TextField();
    private final TextArea ticketMessage = new TextArea();
    private final ComboBox<String> theme = new ComboBox<>();
    private final ComboBox<String> language = new ComboBox<>();
    private final TextField exportFolder = new TextField();
    private final CheckBox printing = new CheckBox("Impression activee");
    private final CheckBox autoBackup = new CheckBox("Sauvegarde automatique");

    public SettingsController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        theme.getItems().setAll("light", "dark");
        language.getItems().setAll("fr");
        ticketMessage.setPrefRowCount(4);
        Button save = primaryButton("Enregistrer", "fas-save");
        save.setOnAction(event -> save());
        HBox body = new HBox(16, section("Restaurant", restaurantForm()), section("Application", appForm()));
        body.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        ScrollPane scrollPane = new ScrollPane(page(toolbar(save), body));
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        load();
        return scrollPane;
    }

    private Node restaurantForm() {
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Nom"), name);
        form.addRow(1, new Label("Adresse"), address);
        form.addRow(2, new Label("Telephone"), phone);
        form.addRow(3, new Label("Email"), email);
        form.addRow(4, new Label("Numero TVA"), vatNumber);
        form.addRow(5, new Label("Devise"), currency);
        form.addRow(6, new Label("TVA par defaut"), vatRate);
        form.addRow(7, new Label("Message ticket"), ticketMessage);
        GridPane.setHgrow(name, Priority.ALWAYS);
        return form;
    }

    private Node appForm() {
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Theme"), theme);
        form.addRow(1, new Label("Langue"), language);
        form.addRow(2, new Label("Dossier export"), exportFolder);
        form.addRow(3, new Label("Impression"), printing);
        form.addRow(4, new Label("Sauvegarde"), autoBackup);
        GridPane.setHgrow(exportFolder, Priority.ALWAYS);
        return form;
    }

    private void load() {
        AppSettings settings = settingsRepository.findAll();
        name.setText(settings.get("restaurant.name", ""));
        address.setText(settings.get("restaurant.address", ""));
        phone.setText(settings.get("restaurant.phone", ""));
        email.setText(settings.get("restaurant.email", ""));
        vatNumber.setText(settings.get("restaurant.vatNumber", ""));
        currency.setText(settings.get("restaurant.currency", "EUR"));
        vatRate.setText(settings.get("restaurant.defaultVatRate", "21"));
        ticketMessage.setText(settings.get("ticket.message", "Merci pour votre visite."));
        theme.setValue(settings.get("app.theme", "light"));
        language.setValue(settings.get("app.language", "fr"));
        exportFolder.setText(settings.get("app.exportFolder", "exports"));
        printing.setSelected(settings.getBoolean("app.printingEnabled", false));
        autoBackup.setSelected(settings.getBoolean("app.autoBackup", true));
    }

    private void save() {
        try {
            String oldTheme = settingsRepository.get("app.theme", "light");
            Map<String, String> values = new LinkedHashMap<>();
            values.put("restaurant.name", name.getText().trim());
            values.put("restaurant.address", address.getText().trim());
            values.put("restaurant.phone", phone.getText().trim());
            values.put("restaurant.email", email.getText().trim());
            values.put("restaurant.vatNumber", vatNumber.getText().trim());
            values.put("restaurant.currency", currency.getText().trim());
            values.put("restaurant.defaultVatRate", vatRate.getText().trim());
            values.put("ticket.message", ticketMessage.getText().trim());
            values.put("app.theme", theme.getValue());
            values.put("app.language", language.getValue());
            values.put("app.exportFolder", exportFolder.getText().trim());
            values.put("app.printingEnabled", Boolean.toString(printing.isSelected()));
            values.put("app.autoBackup", Boolean.toString(autoBackup.isSelected()));
            settingsRepository.saveAll(values);
            if (!oldTheme.equals(theme.getValue())) {
                SessionManager.toggleTheme(layout.getScene());
            }
            showInfo("Parametres enregistres.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }
}
