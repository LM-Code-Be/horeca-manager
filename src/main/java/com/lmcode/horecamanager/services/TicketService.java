package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.models.Order;
import com.lmcode.horecamanager.models.OrderItem;
import com.lmcode.horecamanager.models.Payment;
import com.lmcode.horecamanager.models.Ticket;
import com.lmcode.horecamanager.repositories.SettingsRepository;
import com.lmcode.horecamanager.repositories.TicketRepository;
import com.lmcode.horecamanager.utils.DateUtils;
import com.lmcode.horecamanager.utils.MoneyUtils;
import com.lmcode.horecamanager.utils.PdfUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TicketService {
    private final TicketRepository ticketRepository = new TicketRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();

    public Ticket generateAndSave(Order order, List<OrderItem> items, Payment payment) {
        String number = "T" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + order.id();
        String content = buildContent(number, order, items, payment);
        Ticket ticket = new Ticket(0, order.id(), number, content, order.total(), LocalDateTime.now());
        int id = ticketRepository.save(ticket);
        return new Ticket(id, ticket.orderId(), ticket.ticketNumber(), ticket.content(), ticket.total(), ticket.createdAt());
    }

    public String buildContent(String ticketNumber, Order order, List<OrderItem> items, Payment payment) {
        String restaurant = settingsRepository.get("restaurant.name", "HorecaManager Demo");
        String address = settingsRepository.get("restaurant.address", "");
        String phone = settingsRepository.get("restaurant.phone", "");
        String message = settingsRepository.get("ticket.message", "Merci pour votre visite.");
        StringBuilder builder = new StringBuilder();
        builder.append(center(restaurant, 42)).append('\n');
        if (!address.isBlank()) {
            builder.append(center(address, 42)).append('\n');
        }
        if (!phone.isBlank()) {
            builder.append(center(phone, 42)).append('\n');
        }
        builder.append(line()).append('\n');
        builder.append(row("Ticket", ticketNumber)).append('\n');
        builder.append(row("Date", DateUtils.formatDateTime(LocalDateTime.now()))).append('\n');
        builder.append(row("Commande", "#" + order.id())).append('\n');
        if (order.tableId() != null) {
            builder.append(row("Table", String.valueOf(order.tableId()))).append('\n');
        }
        builder.append(line()).append('\n');
        for (OrderItem item : items) {
            String label = item.quantity() + " x " + item.productName();
            builder.append(row(label, MoneyUtils.format(item.total()))).append('\n');
            builder.append(row("  PU " + MoneyUtils.format(item.unitPrice()) + " TVA " + MoneyUtils.round(item.vatRate()) + "%", "")).append('\n');
        }
        builder.append(line()).append('\n');
        builder.append(row("Total HT", MoneyUtils.format(order.subtotal()))).append('\n');
        builder.append(row("TVA", MoneyUtils.format(order.vatAmount()))).append('\n');
        if (order.discount() > 0) {
            builder.append(row("Remise", MoneyUtils.format(order.discount()))).append('\n');
        }
        builder.append(row("TOTAL TTC", MoneyUtils.format(order.total()))).append('\n');
        builder.append(line()).append('\n');
        builder.append(row("Paiement", payment.method())).append('\n');
        if (payment.amountReceived() != null) {
            builder.append(row("Recu", MoneyUtils.format(payment.amountReceived()))).append('\n');
            builder.append(row("Rendu", MoneyUtils.format(payment.changeDue() == null ? 0 : payment.changeDue()))).append('\n');
        }
        builder.append(line()).append('\n');
        builder.append(center(message, 42)).append('\n');
        return builder.toString();
    }

    public void exportTxt(Ticket ticket, Path path) {
        PdfUtils.writeText(path, ticket.content());
    }

    public void exportPdf(Ticket ticket, Path path) {
        PdfUtils.writePdf(path, "Ticket " + ticket.ticketNumber(), ticket.content());
    }

    private String line() {
        return "-".repeat(42);
    }

    private String row(String left, String right) {
        String cleanLeft = left == null ? "" : left;
        String cleanRight = right == null ? "" : right;
        if (cleanRight.isBlank()) {
            return cleanLeft.length() <= 42 ? cleanLeft : cleanLeft.substring(0, 42);
        }
        int rightLength = Math.min(cleanRight.length(), 18);
        int leftWidth = Math.max(1, 42 - rightLength - 1);
        String trimmedLeft = cleanLeft.length() > leftWidth ? cleanLeft.substring(0, leftWidth - 1) + "." : cleanLeft;
        return String.format("%-" + leftWidth + "s %" + rightLength + "s", trimmedLeft, cleanRight);
    }

    private String center(String text, int width) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() >= width) {
            return text;
        }
        int left = (width - text.length()) / 2;
        return " ".repeat(left) + text;
    }
}
