package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.exception.AccountNotFoundException;
import com.example.bank_transfer_app.exception.InsufficientBalanceException;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.model.TransactionType;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    public static final String BANK_FEE_ACCOUNT = "999999";
    private static final String EXTERNAL_ACCOUNT = "CASH";
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        validateAmount(amount);
        validateCustomerAccount(fromAccount);
        validateCustomerAccount(toAccount);
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Sender and receiver accounts must be different.");
        }

        customerRepository.findByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + fromAccount));
        customerRepository.findByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found: " + toAccount));
        customerRepository.findByAccountNumber(BANK_FEE_ACCOUNT)
                .orElseThrow(() -> new AccountNotFoundException("Bank fee account not found: " + BANK_FEE_ACCOUNT));

        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal debitAmount = amount.add(fee);
        int updatedRows = customerRepository.debit(fromAccount, debitAmount);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAccount + " including the transfer fee.");
        }

        if (simulateFailure) {
            throw new RuntimeException("Simulation system failure after the withdrawal!");
        }

        creditAccount(toAccount, amount);
        creditAccount(BANK_FEE_ACCOUNT, fee);

        return saveTransaction(fromAccount, toAccount, amount, fee, TransactionType.TRANSFER);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        validateCustomerAccount(accountNumber);
        validateAmount(amount);
        customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        creditAccount(accountNumber, amount);
        return saveTransaction(EXTERNAL_ACCOUNT, accountNumber, amount, BigDecimal.ZERO, TransactionType.DEPOSIT);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        validateCustomerAccount(accountNumber);
        validateAmount(amount);
        customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + accountNumber);
        }
        return saveTransaction(accountNumber, EXTERNAL_ACCOUNT, amount, BigDecimal.ZERO, TransactionType.WITHDRAWAL);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        validateCustomerAccount(customer.getAccountNumber());
        return customerRepository.save(customer);
    }

    private void validateCustomerAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()
                || BANK_FEE_ACCOUNT.equals(accountNumber) || EXTERNAL_ACCOUNT.equals(accountNumber)) {
            throw new IllegalArgumentException("A valid customer account number is required.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() > 14) {
            throw new IllegalArgumentException("Amount must be positive, have at most 12 digits, and no more than 2 decimal places.");
        }
    }

    private void creditAccount(String accountNumber, BigDecimal amount) {
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
    }

    private BankTransaction saveTransaction(String fromAccount, String toAccount, BigDecimal amount,
                                            BigDecimal fee, TransactionType type) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setTransactionType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTimestamp(LocalDateTime.now());
        return bankTransactionRepository.save(transaction);
    }
}