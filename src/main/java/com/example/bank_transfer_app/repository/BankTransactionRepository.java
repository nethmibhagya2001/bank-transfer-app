package com.example.bank_transfer_app.repository;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("SELECT t FROM BankTransaction t ORDER BY t.timestamp DESC")
    List<BankTransaction> findAllLatest();

    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :accountNumber OR t.toAccount = :accountNumber ORDER BY t.timestamp DESC")
    List<BankTransaction> findHistory(@Param("accountNumber") String accountNumber);

    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.timestamp DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    @Query("""
            SELECT t FROM BankTransaction t
            WHERE t.status = :status
              AND (t.transactionType = :type OR t.transactionType IS NULL)
              AND t.amount > :minAmount
            ORDER BY t.timestamp DESC
            """)
    List<BankTransaction> findTransfersAboveAmount(
            @Param("minAmount") BigDecimal minAmount,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t
            WHERE t.fromAccount = :account
              AND t.status = :status
              AND (t.transactionType = :type OR t.transactionType IS NULL)
            """)
    BigDecimal totalAmountSentByAccount(
            @Param("account") String account,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type);

    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);
}