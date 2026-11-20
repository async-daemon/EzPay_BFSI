package com.ezpay.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String username,
        String upiId
) {
}
