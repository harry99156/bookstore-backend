package com.pageturner.bookstore.web;

import com.pageturner.bookstore.dto.OrderDtos.CheckoutRequest;
import com.pageturner.bookstore.dto.OrderDtos.OrderResponse;
import com.pageturner.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Checkout and order history")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "Checkout a cart: decrements stock atomically and creates an order")
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        OrderResponse body = OrderResponse.from(service.checkout(request));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.id()).toUri()).body(body);
    }

    @Operation(summary = "Get an order by id")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(service.get(id));
    }

    @Operation(summary = "List a customer's orders, newest first")
    @GetMapping
    public List<OrderResponse> byEmail(@RequestParam String email) {
        return service.findByEmail(email).stream().map(OrderResponse::from).toList();
    }

    @Operation(summary = "Cancel an order and restock its books")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return OrderResponse.from(service.cancel(id));
    }
}
