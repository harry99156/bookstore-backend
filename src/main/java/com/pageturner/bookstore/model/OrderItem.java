package com.pageturner.bookstore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Snapshot of the book at purchase time, so later price/title edits never rewrite order history. */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    OrderItem(CustomerOrder order, Long bookId, String title, BigDecimal unitPrice, int quantity) {
        this.order = order;
        this.bookId = bookId;
        this.title = title;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getBookId() { return bookId; }
    public String getTitle() { return title; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
