package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.utils.MoneyUtils;

public class CalculatorService {
    public double add(double left, double right) {
        return MoneyUtils.round(left + right);
    }

    public double subtract(double left, double right) {
        return MoneyUtils.round(left - right);
    }

    public double multiply(double left, double right) {
        return MoneyUtils.round(left * right);
    }

    public double divide(double left, double right) {
        if (right == 0) {
            throw new IllegalArgumentException("Division par zero.");
        }
        return MoneyUtils.round(left / right);
    }

    public double percentage(double amount, double rate) {
        return MoneyUtils.round(amount * rate / 100.0);
    }

    public double vatAmount(double totalTtc, double vatRate) {
        return MoneyUtils.round(totalTtc - (totalTtc / (1 + vatRate / 100.0)));
    }

    public double discount(double amount, double rate) {
        return MoneyUtils.round(amount * rate / 100.0);
    }

    public double changeDue(double total, double received) {
        return MoneyUtils.round(received - total);
    }
}
