package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.models.Category;
import com.lmcode.horecamanager.models.Product;
import com.lmcode.horecamanager.repositories.CategoryRepository;
import com.lmcode.horecamanager.repositories.ProductRepository;
import com.lmcode.horecamanager.services.ValidationService;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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

import java.time.LocalDateTime;
import java.util.List;

public class ProductsController extends ControllerSupport {
    private final ProductRepository productRepository = new ProductRepository();
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final ValidationService validationService = new ValidationService();
    private final ObservableList<Product> rows = FXCollections.observableArrayList();
    private final TableView<Product> tableView = new TableView<>(rows);
    private final TextField search = new TextField();
    private final ComboBox<Category> categoryFilter = new ComboBox<>();
    private final CheckBox availableOnly = new CheckBox("Disponibles uniquement");
    private final VBox stockAlerts = new VBox(8);

    public ProductsController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        configureTable();
        categoryFilter.setItems(FXCollections.observableArrayList(categoryRepository.findActive()));
        categoryFilter.setPromptText("Categorie");
        categoryFilter.setOnAction(event -> reload());
        search.setPromptText("Nom produit");
        String pendingSearch = SessionManager.consumePendingSearch();
        if (!pendingSearch.isBlank()) {
            search.setText(pendingSearch);
        }
        search.textProperty().addListener((observable, oldValue, newValue) -> reload());
        availableOnly.setSelected(false);
        availableOnly.setOnAction(event -> reload());
        Button add = primaryButton("Ajouter produit", "fas-plus");
        add.setOnAction(event -> editProduct(null));
        Button edit = ghostButton("Modifier", "fas-pen");
        edit.setOnAction(event -> {
            Product product = selected();
            if (product == null) {
                showInfo("Selectionnez un produit.");
            } else {
                editProduct(product);
            }
        });
        Button toggle = ghostButton("Activer/Desactiver", "fas-power-off");
        toggle.setOnAction(event -> toggleAvailability());
        Button restock = ghostButton("Reapprovisionner", "fas-boxes");
        restock.setOnAction(event -> restockSelected());
        Button clear = ghostButton("Effacer filtres", "fas-times");
        clear.setOnAction(event -> {
            search.clear();
            categoryFilter.setValue(null);
            availableOnly.setSelected(false);
            reload();
        });
        HBox tools = toolbar(search, categoryFilter, availableOnly, add, edit, toggle, restock, clear);
        if (!stockAlerts.getStyleClass().contains("section")) {
            stockAlerts.getStyleClass().add("section");
        }
        VBox content = page(tools, stockAlerts, section("Produits", tableView));
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
                column("Nom", Product::name, 180),
                column("Description", product -> safe(product.description()), 260),
                column("Categorie", product -> categoryName(product.categoryId()), 140),
                column("Prix", product -> MoneyUtils.format(product.price()), 100),
                column("TVA", product -> MoneyUtils.round(product.vatRate()) + "%", 80),
                availabilityColumn(),
                column("Stock", this::stockLabel, 150),
                column("Prep.", product -> product.preparationTime() == null ? "" : product.preparationTime() + " min", 90)
        );
        tableView.setPrefHeight(560);
    }

    private TableColumn<Product, String> availabilityColumn() {
        TableColumn<Product, String> column = column("Statut", this::statusLabel, 130);
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

    private void reload() {
        List<Product> products = productRepository.findAll();
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        Integer categoryId = categoryFilter.getValue() == null ? null : categoryFilter.getValue().id();
        rows.setAll(products.stream()
                .filter(product -> categoryId == null || categoryId.equals(product.categoryId()))
                .filter(product -> !availableOnly.isSelected() || product.available())
                .filter(product -> query.isBlank()
                        || contains(product.name(), query)
                        || contains(product.description(), query)
                        || contains(categoryName(product.categoryId()), query)
                        || contains(statusLabel(product), query)
                        || contains(stockLabel(product), query))
                .toList());
        reloadStockAlerts(products);
    }

    private void reloadStockAlerts(List<Product> products) {
        stockAlerts.getChildren().clear();
        List<Product> outOfStock = products.stream()
                .filter(product -> product.stockQuantity() != null && product.stockQuantity() <= 0)
                .toList();
        List<Product> lowStock = products.stream()
                .filter(product -> product.stockQuantity() != null)
                .filter(product -> product.stockQuantity() > 0 && product.stockQuantity() <= ProductRepository.DEFAULT_LOW_STOCK_THRESHOLD)
                .toList();
        if (outOfStock.isEmpty() && lowStock.isEmpty()) {
            stockAlerts.setManaged(false);
            stockAlerts.setVisible(false);
            return;
        }
        stockAlerts.setManaged(true);
        stockAlerts.setVisible(true);
        Label title = new Label("Alertes stock");
        title.getStyleClass().add("section-title");
        stockAlerts.getChildren().add(title);
        outOfStock.stream()
                .limit(4)
                .forEach(product -> stockAlerts.getChildren().add(emptyLabel("Rupture: " + product.name() + " - reapprovisionnement requis.")));
        lowStock.stream()
                .limit(4)
                .forEach(product -> stockAlerts.getChildren().add(emptyLabel("Stock faible: " + product.name() + " - " + product.stockQuantity() + " restant(s).")));
        if (outOfStock.size() + lowStock.size() > 8) {
            stockAlerts.getChildren().add(emptyLabel("Autres alertes visibles dans le tableau produits."));
        }
    }

    private void editProduct(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Nouveau produit" : "Modifier produit");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField name = new TextField(product == null ? "" : product.name());
        TextArea description = new TextArea(product == null ? "" : safe(product.description()));
        description.setPrefRowCount(3);
        ComboBox<Category> category = new ComboBox<>(FXCollections.observableArrayList(categoryRepository.findActive()));
        if (product != null && product.categoryId() != null) {
            categoryRepository.findActive().stream().filter(item -> item.id() == product.categoryId()).findFirst().ifPresent(category::setValue);
        }
        TextField price = new TextField(product == null ? "0" : String.valueOf(product.price()));
        TextField vat = new TextField(product == null ? "21" : String.valueOf(product.vatRate()));
        CheckBox available = new CheckBox("Disponible");
        available.setSelected(product == null || product.available());
        TextField stock = new TextField(product == null || product.stockQuantity() == null ? "" : String.valueOf(product.stockQuantity()));
        TextField prep = new TextField(product == null || product.preparationTime() == null ? "" : String.valueOf(product.preparationTime()));
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Nom"), name);
        form.addRow(1, new Label("Description"), description);
        form.addRow(2, new Label("Categorie"), category);
        form.addRow(3, new Label("Prix"), price);
        form.addRow(4, new Label("TVA"), vat);
        form.addRow(5, new Label("Statut"), available);
        form.addRow(6, new Label("Stock"), stock);
        form.addRow(7, new Label("Preparation"), prep);
        GridPane.setHgrow(name, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            double parsedPrice = MoneyUtils.parseAmount(price.getText());
            double parsedVat = MoneyUtils.parseAmount(vat.getText());
            validationService.validateProduct(name.getText(), parsedPrice, parsedVat);
            return new Product(
                    product == null ? 0 : product.id(),
                    name.getText().trim(),
                    description.getText().trim(),
                    category.getValue() == null ? null : category.getValue().id(),
                    parsedPrice,
                    parsedVat,
                    available.isSelected(),
                    parseNullableInt(stock.getText()),
                    parseNullableInt(prep.getText()),
                    product == null ? LocalDateTime.now() : product.createdAt()
            );
        });
        try {
            dialog.showAndWait().ifPresent(value -> {
                if (product == null) {
                    productRepository.save(value);
                    showInfo("Produit ajoute.");
                } else {
                    productRepository.update(value);
                    showInfo("Produit modifie.");
                }
                reload();
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void toggleAvailability() {
        Product product = selected();
        if (product == null) {
            showInfo("Selectionnez un produit.");
            return;
        }
        try {
            productRepository.updateAvailability(product.id(), !product.available());
            reload();
            showInfo(product.available() ? "Produit indisponible." : "Produit disponible.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void restockSelected() {
        Product product = selected();
        if (product == null) {
            showInfo("Selectionnez un produit.");
            return;
        }
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Reapprovisionner " + product.name());
        ButtonType save = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField quantity = new TextField("10");
        Label current = new Label("Stock actuel: " + (product.stockQuantity() == null ? "non suivi" : product.stockQuantity()));
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Produit"), new Label(product.name()));
        form.addRow(1, new Label("Stock"), current);
        form.addRow(2, new Label("Ajouter"), quantity);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == save ? Integer.parseInt(quantity.getText().trim()) : null);
        try {
            dialog.showAndWait().ifPresent(amount -> {
                productRepository.restock(product.id(), amount);
                reload();
                showInfo("Produit reapprovisionne.");
            });
        } catch (RuntimeException exception) {
            showError(exception);
        }
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

    private String stockLabel(Product product) {
        if (product.stockQuantity() == null) {
            return "Non suivi";
        }
        if (product.stockQuantity() <= 0) {
            return "0 - reapprovisionner";
        }
        if (product.stockQuantity() <= ProductRepository.DEFAULT_LOW_STOCK_THRESHOLD) {
            return product.stockQuantity() + " - seuil bas";
        }
        return String.valueOf(product.stockQuantity());
    }

    private Product selected() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    private String categoryName(Integer categoryId) {
        if (categoryId == null) {
            return "";
        }
        Category category = categoryRepository.findById(categoryId);
        return category == null ? "" : category.name();
    }

    private Integer parseNullableInt(String value) {
        return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
