package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.PaymentRepository;
import com.lmcode.horecamanager.repositories.ReservationRepository;
import com.lmcode.horecamanager.repositories.TicketRepository;
import com.lmcode.horecamanager.utils.MoneyUtils;
import com.lmcode.horecamanager.utils.PdfUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;

public class ReportService {
    private final OrderRepository orderRepository = new OrderRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final TicketRepository ticketRepository = new TicketRepository();

    public String dailySummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ventes du jour: ").append(MoneyUtils.format(paymentRepository.totalToday())).append('\n');
        builder.append("Paiements: ").append(paymentRepository.countToday()).append('\n');
        builder.append("Tickets emis: ").append(ticketRepository.countToday()).append('\n');
        builder.append("Reservations du jour: ").append(reservationRepository.findToday().size()).append('\n');
        builder.append("Panier moyen: ").append(MoneyUtils.format(orderRepository.averageBasketToday())).append('\n');
        return builder.toString();
    }

    public String csvReport() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Type;Nom;Valeur");
        paymentRepository.totalsByMethodToday().forEach((method, total) -> joiner.add("Paiement;" + method + ";" + MoneyUtils.round(total)));
        orderRepository.topProducts().forEach((product, qty) -> joiner.add("Produit;" + product + ";" + qty));
        orderRepository.salesByCategory().forEach((category, total) -> joiner.add("Categorie;" + category + ";" + MoneyUtils.round(total)));
        return joiner.toString();
    }

    public Map<String, Double> paymentsByMethod() {
        return paymentRepository.totalsByMethodToday();
    }

    public Map<String, Double> topProducts() {
        return orderRepository.topProducts();
    }

    public Map<String, Double> salesByCategory() {
        return orderRepository.salesByCategory();
    }

    public void exportCsv(Path path) {
        PdfUtils.writeText(path, csvReport());
    }

    public void exportTxt(Path path) {
        PdfUtils.writeText(path, dailySummary() + "\n" + csvReport());
    }

    public void exportPdf(Path path) {
        PdfUtils.writePdf(path, "Rapport HorecaManager", dailySummary() + "\n" + csvReport());
    }
}
