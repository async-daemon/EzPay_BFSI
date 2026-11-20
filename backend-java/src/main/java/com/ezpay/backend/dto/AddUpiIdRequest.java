package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddUpiIdRequest(@NotBlank @Size(min = 3, max = 100) String upiId) {
}
