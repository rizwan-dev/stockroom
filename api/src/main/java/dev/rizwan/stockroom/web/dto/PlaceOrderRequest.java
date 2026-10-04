package dev.rizwan.stockroom.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PlaceOrderRequest(

        @NotEmpty(message = "an order needs at least one line")
        @Valid
        List<Line> lines) {

    public record Line(
            @NotBlank String sku,
            @Positive int quantity) {
    }
}
