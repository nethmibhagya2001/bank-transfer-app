package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
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
        model.addAttribute("newCustomer", new Customer());
        model.addAttribute("totalBalance", customerRepository.getTotalBalance());
        model.addAttribute("successCount", bankTransactionRepository.countByStatus(com.example.bank_transfer_app.model.TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankTransactionRepository.countByStatus(com.example.bank_transfer_app.model.TransactionStatus.FAILED));
        model.addAttribute("latestTransactions", bankTransactionRepository.findAllLatest());
        model.addAttribute("keyword", keyword);

        return "bank";
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
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,BindingResult bindingResult,RedirectAttributes redirectAttributes) {
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
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Transfer failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("history", bankTransactionRepository.findHistory(accountNumber));
        return "history";
    }
}