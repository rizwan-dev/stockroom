package dev.rizwan.stockroom.service;

import dev.rizwan.stockroom.domain.Product;
import dev.rizwan.stockroom.domain.StockItem;
import dev.rizwan.stockroom.repository.ProductRepository;
import dev.rizwan.stockroom.repository.StockItemRepository;
import dev.rizwan.stockroom.web.dto.CreateProductRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ProductRepository products;
    private final StockItemRepository stock;

    public CatalogService(ProductRepository products, StockItemRepository stock) {
        this.products = products;
        this.stock = stock;
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public List<StockItem> listStock() {
        return stock.findAllWithProduct();
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        Product product = products.save(
                new Product(request.sku(), request.name(), request.unitPriceMinor()));
        stock.save(new StockItem(product, request.initialQuantity()));
        return product;
    }
}
