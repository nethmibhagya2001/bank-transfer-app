package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.model.TransactionType;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BankServiceTest {

    @Autowired
    private BankService bankService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Test
    void transferDepositAndWithdrawalUpdateBalancesAndRecordTypes() {
        String senderAccount = "test-" + UUID.randomUUID();
        String receiverAccount = "test-" + UUID.randomUUID();
        bankService.createCustomer(customer(senderAccount, "1000.00"));
        bankService.createCustomer(customer(receiverAccount, "0.00"));

        BigDecimal feeAccountBalance = balance(BankService.BANK_FEE_ACCOUNT);
        BankTransaction transfer = bankService.transfer(
                senderAccount, receiverAccount, new BigDecimal("100.00"), false);

        assertEquals(TransactionType.TRANSFER, transfer.getTransactionType());
        assertEquals(new BigDecimal("1.00"), transfer.getFee());
        assertEquals(new BigDecimal("899.00"), balance(senderAccount));
        assertEquals(new BigDecimal("100.00"), balance(receiverAccount));
        assertEquals(feeAccountBalance.add(new BigDecimal("1.00")), balance(BankService.BANK_FEE_ACCOUNT));

        bankService.deposit(senderAccount, new BigDecimal("25.00"));
        bankService.withdraw(senderAccount, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("914.00"), balance(senderAccount));
        assertEquals(new BigDecimal("100.00"), bankTransactionRepository.totalAmountSentByAccount(
                senderAccount, TransactionStatus.SUCCESS, TransactionType.TRANSFER));
        assertTrue(bankTransactionRepository.findTransfersAboveAmount(
                new BigDecimal("99.99"), TransactionStatus.SUCCESS, TransactionType.TRANSFER)
                .stream().anyMatch(transaction -> transaction.getId().equals(transfer.getId())));

        var history = bankTransactionRepository.findHistory(senderAccount);
        assertTrue(history.stream().anyMatch(transaction -> transaction.getTransactionType() == TransactionType.DEPOSIT));
        assertTrue(history.stream().anyMatch(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL));
        assertTrue(history.stream().anyMatch(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER
                && new BigDecimal("1.00").equals(transaction.getFee())));
    }

    private Customer customer(String accountNumber, String initialBalance) {
        Customer customer = new Customer();
        customer.setAccountNumber(accountNumber);
        customer.setFullName("Test account");
        customer.setBalance(new BigDecimal(initialBalance));
        return customer;
    }

    private BigDecimal balance(String accountNumber) {
        return customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow()
                .getBalance();
    }
}
