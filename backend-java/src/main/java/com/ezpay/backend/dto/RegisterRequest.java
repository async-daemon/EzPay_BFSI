package com.ezpay.backend.dto;

import jakarta.validation.constraints.*;

/** Use Case 1 / User Story 1.1: registration payload. */
public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String username,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Mobile number must contain digits only") String mobileNumber,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,128}$",
                message = "Password must contain at least one uppercase letter and one digit"
        )
        String password
) {
}
