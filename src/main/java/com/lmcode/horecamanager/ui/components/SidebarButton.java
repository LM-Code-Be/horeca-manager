package com.lmcode.horecamanager.ui.components;

import javafx.scene.control.Button;
import org.kordamp.ikonli.javafx.FontIcon;

public class SidebarButton extends Button {
    public SidebarButton(String text, String iconLiteral) {
        super(text);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("sidebar-icon");
        setGraphic(icon);
        getStyleClass().add("sidebar-button");
        setMaxWidth(Double.MAX_VALUE);
    }

    public void setActive(boolean active) {
        if (active) {
            if (!getStyleClass().contains("active")) {
                getStyleClass().add("active");
            }
        } else {
            getStyleClass().remove("active");
        }
    }
}
