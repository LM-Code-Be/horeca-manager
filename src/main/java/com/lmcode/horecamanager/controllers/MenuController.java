package com.lmcode.horecamanager.controllers;

import com.lmcode.horecamanager.app.SessionManager;
import com.lmcode.horecamanager.models.Category;
import com.lmcode.horecamanager.models.Product;
import com.lmcode.horecamanager.repositories.CategoryRepository;
import com.lmcode.horecamanager.repositories.ProductRepository;
import com.lmcode.horecamanager.ui.components.ProductCard;
import com.lmcode.horecamanager.ui.components.StatusBadge;
import com.lmcode.horecamanager.ui.layout.MainLayout;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class MenuController extends ControllerSupport {
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final FlowPane productsPane = new FlowPane(12, 12);
    private final FlowPane categoriesPane = new FlowPane(8, 8);
    private final TextField search = new TextField();
    private final CheckBox availableOnly = new CheckBox("Disponibles uniquement");
    private Integer selectedCategoryId;

    public MenuController(MainLayout layout) {
        super(layout);
    }

    public Node view() {
        search.setPromptText("Rechercher produit");
        String pendingSearch = SessionManager.consumePendingSearch();
        if (!pendingSearch.isBlank()) {
            search.setText(pendingSearch);
        }
        search.textProperty().addListener((observable, oldValue, newValue) -> reloadProducts());
        availableOnly.setSelected(true);
        availableOnly.setOnAction(event -> reloadProducts());
        Button manage = primaryButton("Gerer produits", "fas-box-open");
        manage.setOnAction(event -> layout.router().navigate("products"));
        HBox tools = toolbar(search, availableOnly, manage);
        productsPane.getStyleClass().add("cards-flow");
        categoriesPane.getStyleClass().add("cards-flow");
        reloadCategories();
        reloadProducts();
        VBox content = page(tools, section("Categories", categoriesPane), section("Carte", productsPane));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private void reloadCategories() {
        categoriesPane.getChildren().clear();
        Button all = ghostButton("Toutes", "fas-layer-group");
        all.setOnAction(event -> {
            selectedCategoryId = null;
            reloadProducts();
        });
        categoriesPane.getChildren().add(all);
        for (Category category : categoryRepository.findActive()) {
            Button button = ghostButton(category.name(), "fas-tag");
            button.setOnAction(event -> {
                selectedCategoryId = category.id();
                reloadProducts();
            });
            categoriesPane.getChildren().add(button);
        }
    }

    private void reloadProducts() {
        productsPane.getChildren().clear();
        List<Product> products = selectedCategoryId == null ? productRepository.findAll() : productRepository.findByCategory(selectedCategoryId);
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        products.stream()
                .filter(product -> !availableOnly.isSelected() || product.available())
                .filter(product -> query.isBlank()
                        || contains(product.name(), query)
                        || contains(product.description(), query)
                        || contains(statusLabel(product), query))
                .forEach(product -> {
                    VBox wrapper = new VBox(8);
                    ProductCard card = new ProductCard(product, item -> showInfo("Produit selectionne: " + item.name()));
                    card.setPrefWidth(230);
                    StatusBadge badge = new StatusBadge(statusLabel(product));
                    wrapper.getChildren().addAll(card, badge);
                    productsPane.getChildren().add(wrapper);
                });
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
