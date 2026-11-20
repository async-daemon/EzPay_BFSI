package com.ezpay.backend.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String userId,
        String username,
        String email,
        String mobileNumber,
        String upiId,
        LocalDateTime registrationDate
) {
}
