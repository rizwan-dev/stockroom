package dev.rizwan.stockroom.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.rizwan.stockroom.TestcontainersConfiguration;
import dev.rizwan.stockroom.domain.Product;
import dev.rizwan.stockroom.domain.StockItem;
import dev.rizwan.stockroom.error.InsufficientStockException;
import dev.rizwan.stockroom.error.StockContentionException;
import dev.rizwan.stockroom.repository.ProductRepository;
import dev.rizwan.stockroom.repository.StockItemRepository;
import dev.rizwan.stockroom.web.dto.PlaceOrderRequest;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The test that justifies the design.
 *
 * <p>Twenty threads try to buy the last few units at the same moment, against a
 * real PostgreSQL. The assertion is not "it usually works" - it is that the
 * number of successful orders exactly equals the stock that existed, and that
 * the final quantity lands on zero and never below. Anything less and the
 * service oversells under load, which is the failure mode that costs real
 * money and is invisible in single-threaded tests.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentOrderTest {

    private static final String SKU = "RACE-001";
    private static final int STOCK = 5;
    private static final int THREADS = 20;

    @Autowired
    private OrderService orders;

    @Autowired
    private ProductRepository products;

    @Autowired
    private StockItemRepository stock;

    @Autowired
    private TransactionTemplate tx;

    /**
     * Seeding happens inside a single transaction on purpose. Each repository
     * call would otherwise run in its own transaction, leaving the saved
     * Product detached by the time it is attached to a new StockItem - which
     * Hibernate rejects with "detached entity passed to persist".
     */
    @BeforeEach
    void seed() {
        tx.executeWithoutResult(status -> {
            products.findBySku(SKU).ifPresent(existing -> {
                stock.findById(existing.getId()).ifPresent(stock::delete);
                products.delete(existing);
            });
            stock.flush();
            products.flush();

            Product product = products.save(new Product(SKU, "Contended item", 1000L));
            stock.save(new StockItem(product, STOCK));
        });
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        PlaceOrderRequest request =
                new PlaceOrderRequest(List.of(new PlaceOrderRequest.Line(SKU, 1)));

        AtomicInteger placed = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        AtomicInteger contended = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();          // release every thread at once
                    orders.place(request);
                    placed.incrementAndGet();
                } catch (InsufficientStockException e) {
                    refused.incrementAndGet();
                } catch (StockContentionException e) {
                    contended.incrementAndGet();
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).as("all threads finished").isTrue();
        pool.shutdownNow();

        Long productId = products.findBySku(SKU).orElseThrow().getId();
        int remaining = stock.findById(productId).orElseThrow().getQuantityOnHand();

        assertThat(unexpected.get())
                .as("no thread failed for an unexpected reason")
                .isZero();
        assertThat(placed.get())
                .as("exactly the available stock was sold, no more and no less")
                .isEqualTo(STOCK);
        assertThat(remaining)
                .as("stock lands exactly on zero, never below")
                .isZero();
        assertThat(placed.get() + refused.get() + contended.get())
                .as("every thread got a definite answer")
                .isEqualTo(THREADS);
    }
}
