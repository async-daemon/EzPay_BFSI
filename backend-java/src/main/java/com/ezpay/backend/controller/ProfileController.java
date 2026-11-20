package com.ezpay.backend.controller;

import com.ezpay.backend.dto.*;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.service.ProfileService;
import com.ezpay.backend.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Use Case 1 / User Story 1.2: Profile Management. */
@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserProfileResponse viewProfile(@CurrentUser User currentUser) {
        return profileService.toProfile(currentUser);
    }

    @PutMapping
    public UserProfileResponse updateProfile(@CurrentUser User currentUser, @Valid @RequestBody UpdateProfileRequest payload) {
        return profileService.updateProfile(currentUser, payload);
    }

    @PostMapping("/upi-id")
    public UserProfileResponse addUpiId(@CurrentUser User currentUser, @Valid @RequestBody AddUpiIdRequest payload) {
        return profileService.addUpiId(currentUser, payload);
    }

    @PostMapping("/bank-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse addBankAccount(@CurrentUser User currentUser, @Valid @RequestBody AddBankAccountRequest payload) {
        return profileService.addBankAccount(currentUser, payload);
    }

    @GetMapping("/bank-accounts")
    public List<BankAccountResponse> listBankAccounts(@CurrentUser User currentUser) {
        return profileService.listBankAccounts(currentUser);
    }
}
