package dev.rizwan.stockroom.domain;

import dev.rizwan.stockroom.error.InsufficientStockException;
import jakarta.persistence.*;

/**
 * Stock level for one product.
 *
 * <p>The {@code @Version} column is what makes concurrent orders safe. Two
 * requests that read the same stock row and both try to decrement it will
 * produce the same version in their UPDATE ... WHERE version = ? clause; the
 * second one matches zero rows and Hibernate raises
 * {@link jakarta.persistence.OptimisticLockException}. One order succeeds, the
 * other is retried. Neither oversells.
 */
@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Version
    @Column(nullable = false)
    private long version;

    protected StockItem() {
        // for JPA
    }

    public StockItem(Product product, int quantityOnHand) {
        this.product = product;
        this.quantityOnHand = quantityOnHand;
    }

    /**
     * Removes stock, or refuses. Deliberately a domain method rather than a
     * setter: the invariant lives with the data it protects, so no caller can
     * decrement without passing this check.
     */
    public void remove(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (quantity > quantityOnHand) {
            throw new InsufficientStockException(product.getSku(), quantity, quantityOnHand);
        }
        quantityOnHand -= quantity;
    }

    public void add(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        quantityOnHand += quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public long getVersion() {
        return version;
    }
}
