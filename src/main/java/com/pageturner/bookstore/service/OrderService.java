package com.pageturner.bookstore.service;

import com.pageturner.bookstore.dto.OrderDtos.CheckoutRequest;
import com.pageturner.bookstore.exception.BusinessRuleException;
import com.pageturner.bookstore.exception.ResourceNotFoundException;
import com.pageturner.bookstore.model.Cart;
import com.pageturner.bookstore.model.CartItem;
import com.pageturner.bookstore.model.CustomerOrder;
import com.pageturner.bookstore.repository.BookRepository;
import com.pageturner.bookstore.repository.CartRepository;
import com.pageturner.bookstore.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository carts;
    private final CartService cartService;
    private final BookRepository books;

    public OrderService(OrderRepository orders, CartRepository carts, CartService cartService,
                        BookRepository books) {
        this.orders = orders;
        this.carts = carts;
        this.cartService = cartService;
        this.books = books;
    }

    /**
     * Atomic checkout: validate stock, decrement it, persist an order snapshot and clear the cart.
     * Any failure rolls the whole transaction back; concurrent buyers are detected via @Version on Book.
     */
    public CustomerOrder checkout(CheckoutRequest r) {
        Cart cart = cartService.get(r.cartId());
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Cart is empty");
        }
        CustomerOrder order = new CustomerOrder(r.customerName(), r.customerEmail(), r.shippingAddress());
        for (CartItem item : cart.getItems()) {
            try {
                item.getBook().decreaseStock(item.getQuantity());
            } catch (IllegalStateException e) {
                throw new BusinessRuleException(e.getMessage());
            }
            order.addItem(item.getBook(), item.getQuantity());
        }
        CustomerOrder saved = orders.save(order);
        cart.getItems().clear();
        carts.save(cart);
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(Long id) {
        return orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findByEmail(String email) {
        return orders.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    /** Cancelling restocks every book in the order. */
    public CustomerOrder cancel(Long id) {
        CustomerOrder order = get(id);
        try {
            order.cancel();
        } catch (IllegalStateException e) {
            throw new BusinessRuleException(e.getMessage());
        }
        // Book may have been deleted since purchase; restock whatever still exists.
        order.getItems().forEach(i -> books.findById(i.getBookId()).ifPresent(b -> b.increaseStock(i.getQuantity())));
        return order;
    }
}
