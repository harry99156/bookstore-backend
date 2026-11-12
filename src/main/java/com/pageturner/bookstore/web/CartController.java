package com.pageturner.bookstore.web;

import com.pageturner.bookstore.dto.CartDtos.AddItemRequest;
import com.pageturner.bookstore.dto.CartDtos.CartResponse;
import com.pageturner.bookstore.dto.CartDtos.UpdateItemRequest;
import com.pageturner.bookstore.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/carts")
@Tag(name = "Cart", description = "Shopping cart lifecycle")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @Operation(summary = "Create an empty cart")
    @PostMapping
    public ResponseEntity<CartResponse> create() {
        CartResponse body = CartResponse.from(service.create());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.id()).toUri()).body(body);
    }

    @Operation(summary = "View a cart")
    @GetMapping("/{cartId}")
    public CartResponse get(@PathVariable String cartId) {
        return CartResponse.from(service.get(cartId));
    }

    @Operation(summary = "Add a book (quantities merge if already present)")
    @PostMapping("/{cartId}/items")
    public CartResponse addItem(@PathVariable String cartId, @Valid @RequestBody AddItemRequest request) {
        return CartResponse.from(service.addItem(cartId, request.bookId(), request.quantity()));
    }

    @Operation(summary = "Set the quantity of a cart line")
    @PutMapping("/{cartId}/items/{bookId}")
    public CartResponse updateItem(@PathVariable String cartId, @PathVariable Long bookId,
                                   @Valid @RequestBody UpdateItemRequest request) {
        return CartResponse.from(service.updateItem(cartId, bookId, request.quantity()));
    }

    @Operation(summary = "Remove a book from the cart")
    @DeleteMapping("/{cartId}/items/{bookId}")
    public CartResponse removeItem(@PathVariable String cartId, @PathVariable Long bookId) {
        return CartResponse.from(service.removeItem(cartId, bookId));
    }
}
