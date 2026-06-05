package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.repositories.SettingsRepository;
import com.lmcode.horecamanager.services.ReportService;
import com.lmcode.horecamanager.ui.components.StatCard;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

public class ReportsController extends ControllerSupport {
    private final ReportService reportService = new ReportService();
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final TextArea summary = new TextArea();
    private final VBox payments = new VBox(8);
    private final VBox products = new VBox(8);
    private final VBox categories = new VBox(8);

    public ReportsController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        Button csv = primaryButton("Exporter CSV", "fas-file-csv");
        csv.setOnAction(event -> export("csv"));
        Button txt = ghostButton("Exporter TXT", "fas-file-alt");
        txt.setOnAction(event -> export("txt"));
        Button pdf = ghostButton("Exporter PDF", "fas-file-pdf");
        pdf.setOnAction(event -> export("pdf"));
        Button refresh = ghostButton("Actualiser", "fas-sync");
        refresh.setOnAction(event -> reload());
        summary.setEditable(false);
        summary.setPrefRowCount(8);
        HBox lists = new HBox(16, section("Paiements par methode", payments), section("Ventes par produit", products), section("Ventes par categorie", categories));
        lists.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        VBox content = page(toolbar(csv, txt, pdf, refresh), reportCards(), section("Resume", summary), lists, section("Recommandations", recommendations()));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        reload();
        return scrollPane;
    }

    private Node reportCards() {
        GridPane cards = new GridPane();
        cards.getStyleClass().add("stats-grid");
        cards.setHgap(14);
        cards.setVgap(14);
        cards.add(new StatCard("Periode", LocalDate.now().toString(), "Rapport du jour", "fas-calendar"), 0, 0);
        cards.add(new StatCard("Exports", "CSV / TXT / PDF", "Dossier configure", "fas-file-export"), 1, 0);
        cards.add(new StatCard("Analyse", "Produits et paiements", "Donnees SQLite", "fas-chart-line"), 2, 0);
        return cards;
    }

    private Node recommendations() {
        VBox box = new VBox(8);
        box.getChildren().add(emptyLabel("Verifier les produits les plus vendus avant le service suivant."));
        box.getChildren().add(emptyLabel("Comparer les montants carte et especes lors de la cloture."));
        return box;
    }

    private void reload() {
        summary.setText(reportService.dailySummary());
        fill(payments, reportService.paymentsByMethod(), true);
        fill(products, reportService.topProducts(), false);
        fill(categories, reportService.salesByCategory(), true);
    }

    private void fill(VBox target, Map<String, Double> rows, boolean money) {
        target.getChildren().clear();
        if (rows.isEmpty()) {
            target.getChildren().add(emptyLabel("Aucune donnee."));
            return;
        }
        rows.forEach((label, value) -> target.getChildren().add(emptyLabel(label + "  " + (money ? MoneyUtils.format(value) : String.valueOf(value.intValue())))));
    }

    private void export(String type) {
        try {
            String folder = settingsRepository.get("app.exportFolder", "exports");
            Path path = Path.of(folder, "rapport-" + LocalDate.now() + "." + type);
            switch (type) {
                case "csv" -> reportService.exportCsv(path);
                case "txt" -> reportService.exportTxt(path);
                case "pdf" -> reportService.exportPdf(path);
                default -> throw new IllegalArgumentException("Format invalide.");
            }
            showInfo("Rapport exporte: " + path);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }
}
