package dev.rizwan.stockroom.web.dto;

import dev.rizwan.stockroom.domain.Order;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String reference,
        String status,
        long totalMinor,
        Instant createdAt,
        List<Line> lines) {

    public record Line(String sku, String name, int quantity, long unitPriceMinor, long lineTotalMinor) {
    }

    public static OrderResponse from(Order order) {
        List<Line> lines = order.getLines().stream()
                .map(l -> new Line(
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getUnitPriceMinor(),
                        l.getLineTotalMinor()))
                .toList();
        return new OrderResponse(order.getReference(), order.getStatus().name(),
                order.getTotalMinor(), order.getCreatedAt(), lines);
    }
}
