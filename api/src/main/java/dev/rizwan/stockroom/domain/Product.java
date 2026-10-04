package dev.rizwan.stockroom.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

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

    protected Product() {
        // for JPA
    }

    public Product(String sku, String name, long unitPriceMinor) {
        this.sku = sku;
        this.name = name;
        this.unitPriceMinor = unitPriceMinor;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
