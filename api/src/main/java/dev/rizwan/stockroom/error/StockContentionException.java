package dev.rizwan.stockroom.error;

/**
 * Thrown when concurrent orders kept colliding on the same stock rows and the
 * retries ran out.
 *
 * <p>Deliberately distinct from {@link InsufficientStockException}: "we could
 * not complete this right now, try again" is a different answer from "there is
 * not enough stock", and collapsing the two into one generic error is how
 * clients end up telling users the wrong thing.
 */
public class StockContentionException extends RuntimeException {

    public StockContentionException(String message) {
        super(message);
    }
}
