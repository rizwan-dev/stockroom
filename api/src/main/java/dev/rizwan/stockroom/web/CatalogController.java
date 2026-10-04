package dev.rizwan.stockroom.web;

import dev.rizwan.stockroom.service.CatalogService;
import dev.rizwan.stockroom.web.dto.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public List<ProductResponse> products() {
        return catalog.listProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/stock")
    public List<StockResponse> stock() {
        return catalog.listStock().stream().map(StockResponse::from).toList();
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = ProductResponse.from(catalog.create(request));
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }
}
