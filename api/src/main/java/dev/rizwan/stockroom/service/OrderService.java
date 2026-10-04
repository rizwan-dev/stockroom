package dev.rizwan.stockroom.service;

import dev.rizwan.stockroom.domain.Order;
import dev.rizwan.stockroom.error.NotFoundException;
import dev.rizwan.stockroom.error.StockContentionException;
import dev.rizwan.stockroom.repository.OrderRepository;
import dev.rizwan.stockroom.web.dto.PlaceOrderRequest;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /**
     * How many times one order re-attempts after losing an optimistic lock
     * race. Three rides out normal contention without turning a genuinely hot
     * row into an unbounded retry storm — past that, failing fast and telling
     * the caller to retry is the more honest answer.
     */
    private static final int MAX_ATTEMPTS = 3;

    private final OrderPlacer placer;
    private final OrderRepository orders;

    public OrderService(OrderPlacer placer, OrderRepository orders) {
        this.placer = placer;
        this.orders = orders;
    }

    /**
     * Places an order, decrementing stock atomically.
     *
     * <p>Note there is no {@code @Transactional} on this method. The retry loop
     * must sit outside any transaction: a transaction that has already hit an
     * optimistic lock failure is marked rollback-only, so retrying within it
     * would fail every time. Each attempt gets its own transaction via
     * {@link OrderPlacer}, which is a separate bean so that the call is proxied.
     */
    public Order place(PlaceOrderRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return placer.attempt(request);
            } catch (OptimisticLockingFailureException | OptimisticLockException e) {
                log.debug("Optimistic lock lost placing order, attempt {} of {}", attempt, MAX_ATTEMPTS);
            }
        }

        log.warn("Giving up on an order after {} contended attempts", MAX_ATTEMPTS);
        throw new StockContentionException(
                ("Could not place the order after %d attempts because stock was being changed "
                        + "concurrently. Nothing was reserved; please retry.").formatted(MAX_ATTEMPTS));
    }

    @Transactional(readOnly = true)
    public Order findByReference(String reference) {
        Order order = orders.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("No order with reference " + reference));
        order.getLines().forEach(line -> line.getProduct().getSku()); // initialise lazy refs
        return order;
    }
}
