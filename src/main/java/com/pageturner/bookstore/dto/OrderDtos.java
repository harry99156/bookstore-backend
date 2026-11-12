package com.pageturner.bookstore.dto;

import com.pageturner.bookstore.model.CustomerOrder;
import com.pageturner.bookstore.model.OrderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutRequest(
            @NotNull String cartId,
            @NotBlank @Size(max = 255) String customerName,
            @NotBlank @Email String customerEmail,
            @NotBlank @Size(max = 500) String shippingAddress) {
    }

    public record OrderItemResponse(Long bookId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record OrderResponse(Long id, String customerName, String customerEmail, String shippingAddress,
                                OrderStatus status, BigDecimal total, Instant createdAt,
                                List<OrderItemResponse> items) {
        public static OrderResponse from(CustomerOrder o) {
            return new OrderResponse(o.getId(), o.getCustomerName(), o.getCustomerEmail(), o.getShippingAddress(),
                    o.getStatus(), o.getTotal(), o.getCreatedAt(),
                    o.getItems().stream()
                            .map(i -> new OrderItemResponse(i.getBookId(), i.getTitle(), i.getUnitPrice(),
                                    i.getQuantity(), i.lineTotal()))
                            .toList());
        }
    }
}
