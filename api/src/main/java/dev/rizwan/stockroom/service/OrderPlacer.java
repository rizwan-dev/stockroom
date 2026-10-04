package dev.rizwan.stockroom.service;

import dev.rizwan.stockroom.domain.Order;
import dev.rizwan.stockroom.domain.Product;
import dev.rizwan.stockroom.domain.StockItem;
import dev.rizwan.stockroom.error.NotFoundException;
import dev.rizwan.stockroom.repository.OrderRepository;
import dev.rizwan.stockroom.repository.ProductRepository;
import dev.rizwan.stockroom.repository.StockItemRepository;
import dev.rizwan.stockroom.web.dto.PlaceOrderRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One attempt at placing an order, in its own transaction.
 *
 * <p>This is a separate bean from {@link OrderService} on purpose, and the
 * reason is the single most common way Spring transactions are got wrong.
 *
 * <p>{@code @Transactional} is implemented with a proxy. When a method on a
 * bean calls another method on <em>the same instance</em>, the call goes
 * straight to the target object and never passes through that proxy, so the
 * annotation is silently ignored. A retry loop calling a private or same-class
 * {@code @Transactional} method would therefore run every attempt inside the
 * caller's original transaction — which, after an optimistic lock failure, is
 * already marked rollback-only. Every retry would fail, and the code would look
 * correct while being useless.
 *
 * <p>Putting the attempt in its own bean means the call is a real proxied call,
 * so REQUIRES_NEW genuinely starts a fresh transaction each time.
 */
@Component
public class OrderPlacer {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final StockItemRepository stock;

    public OrderPlacer(OrderRepository orders, ProductRepository products, StockItemRepository stock) {
        this.orders = orders;
        this.products = products;
        this.stock = stock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order attempt(PlaceOrderRequest request) {
        Order order = Order.placed();

        for (PlaceOrderRequest.Line line : request.lines()) {
            Product product = products.findBySku(line.sku())
                    .orElseThrow(() -> new NotFoundException("No product with SKU " + line.sku()));

            StockItem item = stock.findById(product.getId())
                    .orElseThrow(() -> new NotFoundException("No stock record for SKU " + line.sku()));

            // Throws InsufficientStockException when the quantity is not there.
            // That is a business answer and is not retried.
            item.remove(line.quantity());
            order.addLine(product, line.quantity());
        }

        // saveAndFlush, not save: we want the optimistic-lock check to happen
        // here, inside this try, rather than at an ambiguous point during
        // transaction commit after the retry loop has already moved on.
        return orders.saveAndFlush(order);
    }
}
