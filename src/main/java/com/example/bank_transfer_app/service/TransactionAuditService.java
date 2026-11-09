package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository bankTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String fromAccount, String toAccount, BigDecimal amount, String errorMessage) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setTimestamp(LocalDateTime.now());
        
        bankTransactionRepository.save(transaction);
    }
}