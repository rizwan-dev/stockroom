package dev.rizwan.stockroom.web.dto;

import dev.rizwan.stockroom.domain.StockItem;

public record StockResponse(String sku, String name, int quantityOnHand, long unitPriceMinor) {

    public static StockResponse from(StockItem item) {
        return new StockResponse(
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantityOnHand(),
                item.getProduct().getUnitPriceMinor());
    }
}
