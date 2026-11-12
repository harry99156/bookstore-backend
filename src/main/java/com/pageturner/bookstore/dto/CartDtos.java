package com.pageturner.bookstore.dto;

import com.pageturner.bookstore.model.Cart;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotNull Long bookId, @Min(1) @Max(100) int quantity) {
    }

    public record UpdateItemRequest(@Min(1) @Max(100) int quantity) {
    }

    public record CartItemResponse(Long bookId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record CartResponse(String id, List<CartItemResponse> items, BigDecimal total) {
        public static CartResponse from(Cart cart) {
            List<CartItemResponse> items = cart.getItems().stream()
                    .map(i -> new CartItemResponse(i.getBook().getId(), i.getBook().getTitle(),
                            i.getBook().getPrice(), i.getQuantity(), i.lineTotal()))
                    .toList();
            return new CartResponse(cart.getId(), items, cart.total());
        }
    }
}
