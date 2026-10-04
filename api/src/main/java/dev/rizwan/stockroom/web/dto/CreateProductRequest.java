package dev.rizwan.stockroom.web.dto;

import jakarta.validation.constraints.*;

public record CreateProductRequest(

        @NotBlank
        @Pattern(regexp = "[A-Z0-9-]{3,64}", message = "sku must be uppercase letters, digits and hyphens")
        String sku,

        @NotBlank
        @Size(max = 255)
        String name,

        @Positive
        long unitPriceMinor,

        @PositiveOrZero
        int initialQuantity) {
}
