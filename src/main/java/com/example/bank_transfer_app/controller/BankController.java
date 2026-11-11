package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.dto.AccountAmountRequest;
import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.model.TransactionType;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("customers", customerRepository.searchByName(keyword));
        } else {
            model.addAttribute("customers", customerRepository.findAllOrdered());
        }

        model.addAttribute("transferRequest", new TransferRequest());
        model.addAttribute("depositRequest", new AccountAmountRequest());
        model.addAttribute("withdrawalRequest", new AccountAmountRequest());
        model.addAttribute("newCustomer", new Customer());
        model.addAttribute("totalBalance", customerRepository.getTotalBalance());
        model.addAttribute("successCount", bankTransactionRepository.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankTransactionRepository.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("latestTransactions", bankTransactionRepository.findAllLatest());
        model.addAttribute("keyword", keyword);

        return "bank";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000.00") BigDecimal min, Model model) {
        model.addAttribute("minAmount", min);
        model.addAttribute("transfers", bankTransactionRepository.findTransfersAboveAmount(
                min, TransactionStatus.SUCCESS, TransactionType.TRANSFER));
        return "large-transfers";
    }

    @PostMapping("/customers")
    public String addCustomer(@ModelAttribute("newCustomer") Customer customer, RedirectAttributes redirectAttributes) {
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding customer: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error in transfer form.");
            return "redirect:/";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully!");
        } catch (Exception e) {
            try {
                auditService.recordFailure(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        e.getMessage()
                );
            } catch (Exception auditEx) {
                log.error("Failed to record failed transfer from {} to {}",
                        request.getFromAccount(), request.getToAccount(), auditEx);
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Transfer failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute("depositRequest") AccountAmountRequest request,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error in deposit form.");
            return "redirect:/";
        }

        try {
            bankService.deposit(request.getAccountNumber(), request.getAmount());
            redirectAttributes.addFlashAttribute("successMessage", "Deposit completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Deposit failed: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(@Valid @ModelAttribute("withdrawalRequest") AccountAmountRequest request,
                           BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error in withdrawal form.");
            return "redirect:/";
        }

        try {
            bankService.withdraw(request.getAccountNumber(), request.getAmount());
            redirectAttributes.addFlashAttribute("successMessage", "Withdrawal completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Withdrawal failed: " + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("history", bankTransactionRepository.findHistory(accountNumber));
        model.addAttribute("totalSent", bankTransactionRepository.totalAmountSentByAccount(
                accountNumber, TransactionStatus.SUCCESS, TransactionType.TRANSFER));
        return "history";
    }
}