package com.ezpay.backend.controller;

import com.ezpay.backend.dto.CreateReminderRequest;
import com.ezpay.backend.dto.ReminderResponse;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.service.ReminderService;
import com.ezpay.backend.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Use Case 4 / User Story 4.2: Payment Reminders. */
@RestController
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@CurrentUser User currentUser, @Valid @RequestBody CreateReminderRequest payload) {
        return reminderService.create(currentUser, payload);
    }

    @GetMapping
    public List<ReminderResponse> list(@CurrentUser User currentUser) {
        return reminderService.list(currentUser);
    }

    @PostMapping("/{reminderId}/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendNow(@CurrentUser User currentUser, @PathVariable String reminderId) {
        reminderService.sendNow(currentUser, reminderId);
    }

    @PostMapping("/{reminderId}/mark-paid")
    public ReminderResponse markPaid(@CurrentUser User currentUser, @PathVariable String reminderId) {
        return reminderService.markPaid(currentUser, reminderId);
    }
}
