package dev.rizwan.stockroom.web;

import dev.rizwan.stockroom.service.OrderService;
import dev.rizwan.stockroom.web.dto.OrderResponse;
import dev.rizwan.stockroom.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse created = OrderResponse.from(orders.place(request));
        return ResponseEntity.created(URI.create("/api/orders/" + created.reference())).body(created);
    }

    @GetMapping("/{reference}")
    public OrderResponse get(@PathVariable String reference) {
        return OrderResponse.from(orders.findByReference(reference));
    }
}
