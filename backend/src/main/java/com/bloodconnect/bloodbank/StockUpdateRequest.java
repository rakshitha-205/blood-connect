package com.bloodconnect.bloodbank;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record StockUpdateRequest(
        @NotNull(message = "Stock is required.")
        Map<String, @NotNull @Min(value = 0, message = "Units can't be negative.")
                @Max(value = 10000, message = "Units must be at most 10000.") Integer> stock
) {
}
