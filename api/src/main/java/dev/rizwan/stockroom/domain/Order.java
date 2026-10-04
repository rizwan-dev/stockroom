package dev.rizwan.stockroom.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "total_minor", nullable = false)
    private long totalMinor;

    /**
     * Written by the database default, then read back into the entity.
     *
     * <p>Without {@code @Generated(INSERT)} Hibernate never re-reads the row
     * after inserting it, so this field stays null for the lifetime of the
     * persistence context and the API happily serialises a null for a column
     * that is NOT NULL in the database. The database stays the source of truth
     * for time, which matters once more than one process writes.
     */
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
        // for JPA
    }

    /**
     * Static factory rather than a public no-arg constructor: JPA needs the
     * no-arg one for itself, and a second one with the same signature will not
     * compile. Naming the intent also reads better at the call site.
     */
    public static Order placed() {
        Order order = new Order();
        order.reference = UUID.randomUUID().toString();
        order.status = OrderStatus.PLACED;
        return order;
    }

    public void addLine(Product product, int quantity) {
        lines.add(new OrderLine(this, product, quantity));
        recalculateTotal();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    private void recalculateTotal() {
        this.totalMinor = lines.stream().mapToLong(OrderLine::getLineTotalMinor).sum();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTotalMinor() {
        return totalMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }
}
