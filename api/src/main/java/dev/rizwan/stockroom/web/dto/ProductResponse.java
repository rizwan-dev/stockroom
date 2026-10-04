package dev.rizwan.stockroom.web.dto;

import dev.rizwan.stockroom.domain.Product;

public record ProductResponse(Long id, String sku, String name, long unitPriceMinor) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(),
                product.getName(), product.getUnitPriceMinor());
    }
}
