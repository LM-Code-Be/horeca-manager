package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.models.CashSession;
import com.lmcode.horecamanager.repositories.CashSessionRepository;
import com.lmcode.horecamanager.repositories.PaymentRepository;
import com.lmcode.horecamanager.utils.ValidationUtils;

import java.util.Map;

public class CashRegisterService {
    private final CashSessionRepository cashSessionRepository = new CashSessionRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public CashSession currentSession() {
        CashSession open = cashSessionRepository.findOpen();
        return open == null ? cashSessionRepository.findLatest() : open;
    }

    public void open(double initialAmount, String note) {
        ValidationUtils.requireNonNegative(initialAmount, "Montant initial invalide.");
        if (cashSessionRepository.isOpen()) {
            throw new IllegalArgumentException("Caisse deja ouverte.");
        }
        cashSessionRepository.open(initialAmount, note);
    }

    public void close(double realAmount, String note) {
        CashSession session = cashSessionRepository.findOpen();
        if (session == null) {
            throw new IllegalArgumentException("Aucune caisse ouverte.");
        }
        ValidationUtils.requireNonNegative(realAmount, "Montant reel invalide.");
        cashSessionRepository.close(session.id(), realAmount, note);
    }

    public boolean isOpen() {
        return cashSessionRepository.isOpen();
    }

    public double totalToday() {
        return paymentRepository.totalToday();
    }

    public long paymentCountToday() {
        return paymentRepository.countToday();
    }

    public Map<String, Double> totalsByMethodToday() {
        return paymentRepository.totalsByMethodToday();
    }
}
