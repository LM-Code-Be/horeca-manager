package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.models.OrderItem;
import com.lmcode.horecamanager.utils.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

public class ValidationService {
    public void validateProduct(String name, double price, double vatRate) {
        ValidationUtils.requireText(name, "Nom produit requis.");
        ValidationUtils.requirePositive(price, "Prix invalide.");
        ValidationUtils.requireNonNegative(vatRate, "TVA invalide.");
    }

    public void validateTable(String number, int capacity) {
        ValidationUtils.requireText(number, "Numero de table requis.");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacite invalide.");
        }
    }

    public void validateReservation(String customerName, String phone, String email, LocalDate date, int guests) {
        ValidationUtils.requireText(customerName, "Nom client requis.");
        ValidationUtils.validatePhone(phone);
        ValidationUtils.validateEmail(email);
        ValidationUtils.validateReservationDate(date);
        if (guests <= 0) {
            throw new IllegalArgumentException("Nombre de personnes invalide.");
        }
    }

    public void validateOrderCanBePaid(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Commande vide.");
        }
    }

    public void validateCashPayment(double total, double received) {
        if (received < total) {
            throw new IllegalArgumentException("Montant recu insuffisant.");
        }
    }
}
