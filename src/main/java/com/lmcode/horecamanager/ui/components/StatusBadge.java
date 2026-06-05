package com.lmcode.horecamanager.ui.components;

import javafx.scene.control.Label;

public class StatusBadge extends Label {
    public StatusBadge(String status) {
        getStyleClass().add("status-badge");
        setStatus(status);
    }

    public void setStatus(String status) {
        setText(format(status));
        getStyleClass().removeIf(style -> style.startsWith("status-") && !"status-badge".equals(style));
        getStyleClass().add("status-" + normalize(status));
    }

    private String format(String status) {
        return status == null ? "" : status.replace('_', ' ');
    }

    private String normalize(String status) {
        return status == null ? "neutral" : status.toLowerCase().replace('_', '-').replace(' ', '-');
    }
}
