package com.lmcode.horecamanager.ui.components;

import javafx.scene.control.TextArea;

public class TicketPreview extends TextArea {
    public TicketPreview() {
        getStyleClass().add("ticket-preview");
        setEditable(false);
        setWrapText(true);
        setPrefColumnCount(38);
    }
}
