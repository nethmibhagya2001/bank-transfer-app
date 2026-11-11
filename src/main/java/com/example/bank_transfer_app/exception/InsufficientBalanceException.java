package com.example.bank_transfer_app.exception;

public class InsufficientBalanceException extends BankException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}