package com.lmcode.horecamanager.ui.components;

import com.lmcode.horecamanager.models.Product;
import com.lmcode.horecamanager.repositories.ProductRepository;
import com.lmcode.horecamanager.utils.MoneyUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public class ProductCard extends VBox {
    public ProductCard(Product product, Consumer<Product> action) {
        getStyleClass().add("product-card");
        setSpacing(8);
        Label name = new Label(product.name());
        name.getStyleClass().add("item-title");
        Label description = new Label(product.description() == null ? "" : product.description());
        description.getStyleClass().add("muted");
        description.setWrapText(true);
        Label meta = new Label(meta(product));
        meta.getStyleClass().add("product-meta");
        Label price = new Label(MoneyUtils.format(product.price()));
        price.getStyleClass().add("price");
        Button add = new Button("Ajouter", new FontIcon("fas-plus"));
        add.getStyleClass().add("primary-button");
        add.setDisable(!product.available());
        add.setOnAction(event -> action.accept(product));
        HBox bottom = new HBox(price, add);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setSpacing(12);
        getChildren().addAll(name, description, meta, bottom);
    }

    private String meta(Product product) {
        String stock;
        if (product.stockQuantity() == null) {
            stock = "Stock non suivi";
        } else if (product.stockQuantity() <= 0) {
            stock = "Rupture - reapprovisionner";
        } else if (product.stockQuantity() <= ProductRepository.DEFAULT_LOW_STOCK_THRESHOLD) {
            stock = "Stock faible: " + product.stockQuantity();
        } else {
            stock = "Stock " + product.stockQuantity();
        }
        String prep = product.preparationTime() == null ? "Preparation libre" : product.preparationTime() + " min";
        return stock + " - " + prep + " - TVA " + MoneyUtils.round(product.vatRate()) + "%";
    }
}
