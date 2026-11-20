package com.ezpay.backend.controller;

import com.ezpay.backend.dto.TransactionResponse;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.service.TransactionService;
import com.ezpay.backend.web.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Use Case 3: Transaction Management (3.1 history, 3.2 status tracking). */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> history(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        return transactionService.history(currentUser, type, status, dateFrom, dateTo);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse track(@CurrentUser User currentUser, @PathVariable String transactionId) {
        return transactionService.track(currentUser, transactionId);
    }
}
