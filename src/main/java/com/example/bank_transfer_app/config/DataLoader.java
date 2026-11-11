package com.example.bank_transfer_app.config;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.repository.CustomerRepository;
import com.example.bank_transfer_app.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) throws Exception {
        if (customerRepository.count() == 0) {
            Customer c1 = new Customer();
            c1.setAccountNumber("100001");
            c1.setFullName("Kamal");
            c1.setBalance(new BigDecimal("50000.00"));
            customerRepository.save(c1);

            Customer c2 = new Customer();
            c2.setAccountNumber("100002");
            c2.setFullName("Nimali");
            c2.setBalance(new BigDecimal("25000.00"));
            customerRepository.save(c2);

            Customer c3 = new Customer();
            c3.setAccountNumber("100003");
            c3.setFullName("Saman");
            c3.setBalance(new BigDecimal("10000.00"));
            customerRepository.save(c3);

            System.out.println(">>> Sample data loaded successfully! Total bank money: LKR 85,000.00 <<<");
        }

        if (customerRepository.findByAccountNumber(BankService.BANK_FEE_ACCOUNT).isEmpty()) {
            Customer bankFeeAccount = new Customer();
            bankFeeAccount.setAccountNumber(BankService.BANK_FEE_ACCOUNT);
            bankFeeAccount.setFullName("Bank Fee Account");
            bankFeeAccount.setBalance(BigDecimal.ZERO);
            customerRepository.save(bankFeeAccount);
        }
    }
}