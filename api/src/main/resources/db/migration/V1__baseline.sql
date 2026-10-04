-- Stockroom baseline schema.
--
-- Money is stored in minor units (paise/cents) as BIGINT. Floating point money
-- is a bug waiting for a rounding edge case.

CREATE TABLE products (
    id               BIGSERIAL PRIMARY KEY,
    sku              VARCHAR(64)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    unit_price_minor BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT products_sku_unique     UNIQUE (sku),
    CONSTRAINT products_price_positive CHECK (unit_price_minor > 0)
);

-- Stock is a separate table from products on purpose: a product is reference
-- data that rarely changes, stock is contended and updated constantly. Keeping
-- them apart means an order does not take a write lock on product metadata.
CREATE TABLE stock_items (
    product_id       BIGINT  PRIMARY KEY REFERENCES products (id) ON DELETE CASCADE,
    quantity_on_hand INTEGER NOT NULL,
    version          BIGINT  NOT NULL DEFAULT 0,

    -- The database refuses to hold negative stock. Application code can be
    -- wrong; this constraint cannot be bypassed by a race, a retry or a new
    -- caller that forgot the check.
    CONSTRAINT stock_not_negative CHECK (quantity_on_hand >= 0)
);

CREATE TABLE orders (
    id         BIGSERIAL PRIMARY KEY,
    reference  VARCHAR(36) NOT NULL,
    status     VARCHAR(16) NOT NULL,
    total_minor BIGINT     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT orders_reference_unique UNIQUE (reference),
    CONSTRAINT orders_status_valid     CHECK (status IN ('PLACED', 'CANCELLED'))
);

CREATE TABLE order_lines (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT  NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id       BIGINT  NOT NULL REFERENCES products (id),
    quantity         INTEGER NOT NULL,
    unit_price_minor BIGINT  NOT NULL,

    CONSTRAINT order_lines_quantity_positive CHECK (quantity > 0),
    -- One line per product per order; two lines for the same product is a
    -- client bug that would otherwise silently double-decrement stock.
    CONSTRAINT order_lines_unique_product    UNIQUE (order_id, product_id)
);

CREATE INDEX order_lines_order_id_idx ON order_lines (order_id);
