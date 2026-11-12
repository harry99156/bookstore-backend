package com.pageturner.bookstore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    public String getId() { return id; }
    public List<CartItem> getItems() { return items; }

    public void addOrIncrease(Book book, int quantity) {
        items.stream()
                .filter(i -> i.getBook().getId().equals(book.getId()))
                .findFirst()
                .ifPresentOrElse(
                        i -> i.setQuantity(i.getQuantity() + quantity),
                        () -> items.add(new CartItem(this, book, quantity)));
    }

    public BigDecimal total() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
