package com.pageturner.bookstore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {
    }

    CartItem(Cart cart, Book book, int quantity) {
        this.cart = cart;
        this.book = book;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal lineTotal() {
        return book.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
