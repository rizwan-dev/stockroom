package dev.rizwan.stockroom.error;

/**
 * Thrown when an order asks for more of a product than exists.
 *
 * <p>This is a business outcome, not a failure. It carries the numbers so the
 * API can tell the caller what is actually available instead of a bare 409.
 */
public class InsufficientStockException extends RuntimeException {

    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for %s: requested %d, available %d"
                .formatted(sku, requested, available));
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

    public String getSku() {
        return sku;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
