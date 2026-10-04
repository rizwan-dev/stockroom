-- Demo data so the UI has something to show on first run.
INSERT INTO products (sku, name, unit_price_minor) VALUES
    ('DESK-001',  'Standing desk',        4999900),
    ('CHAIR-001', 'Ergonomic chair',      2499900),
    ('LAMP-001',  'Desk lamp',             349900),
    ('CABLE-001', 'USB-C cable, 2m',        99900),
    ('MON-001',   'Monitor, 27 inch',     3299900);

INSERT INTO stock_items (product_id, quantity_on_hand)
SELECT id, 25 FROM products;
