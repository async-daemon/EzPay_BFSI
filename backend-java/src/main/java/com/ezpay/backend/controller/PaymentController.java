package com.ezpay.backend.controller;

import com.ezpay.backend.dto.BankTransferRequest;
import com.ezpay.backend.dto.TransactionResponse;
import com.ezpay.backend.dto.UpiPaymentRequest;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.service.PaymentService;
import com.ezpay.backend.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Use Case 2: Payment Processing (User stories 2.1 UPI, 2.2 Bank Transfer). */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/upi")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse payUpi(@CurrentUser User currentUser, @Valid @RequestBody UpiPaymentRequest payload) {
        return paymentService.payUpi(currentUser, payload);
    }

    @PostMapping("/bank-transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse bankTransfer(@CurrentUser User currentUser, @Valid @RequestBody BankTransferRequest payload) {
        return paymentService.bankTransfer(currentUser, payload);
    }
}
