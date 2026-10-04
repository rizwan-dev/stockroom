package dev.rizwan.stockroom.domain;

import static org.assertj.core.api.Assertions.*;

import dev.rizwan.stockroom.error.InsufficientStockException;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests. No Spring, no database - the invariant lives in the entity,
 * so it can be tested in microseconds.
 */
class StockItemTest {

    private StockItem stockOf(int quantity) {
        return new StockItem(new Product("SKU-001", "Thing", 1000L), quantity);
    }

    @Test
    void removesAvailableStock() {
        StockItem item = stockOf(10);
        item.remove(4);
        assertThat(item.getQuantityOnHand()).isEqualTo(6);
    }

    @Test
    void allowsRemovingExactlyWhatIsLeft() {
        StockItem item = stockOf(3);
        item.remove(3);
        assertThat(item.getQuantityOnHand()).isZero();
    }

    @Test
    void refusesToOversellAndSaysByHowMuch() {
        StockItem item = stockOf(2);

        assertThatThrownBy(() -> item.remove(5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 5")
                .hasMessageContaining("available 2");

        assertThat(item.getQuantityOnHand())
                .as("a refused removal must not change the stock level")
                .isEqualTo(2);
    }

    @Test
    void rejectsNonPositiveQuantities() {
        StockItem item = stockOf(10);
        assertThatThrownBy(() -> item.remove(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item.remove(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
