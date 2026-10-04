package dev.rizwan.stockroom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void totalsLinesUsingThePriceAtOrderTime() {
        Order order = Order.placed();
        order.addLine(new Product("A", "Desk", 4999900L), 2);
        order.addLine(new Product("B", "Lamp", 349900L), 3);

        assertThat(order.getTotalMinor()).isEqualTo(2 * 4999900L + 3 * 349900L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getReference()).isNotBlank();
    }

    @Test
    void linesAreNotMutableFromOutside() {
        Order order = Order.placed();
        order.addLine(new Product("A", "Desk", 100L), 1);

        assertThat(order.getLines()).hasSize(1);
        assertThatExceptionIsThrownWhenMutating(order);
    }

    private void assertThatThrows(Runnable r) {
        try {
            r.run();
            throw new AssertionError("expected the returned list to be immutable");
        } catch (UnsupportedOperationException expected) {
            // good
        }
    }

    private void assertThatExceptionIsThrownWhenMutating(Order order) {
        assertThatThrows(() -> order.getLines().clear());
    }
}
