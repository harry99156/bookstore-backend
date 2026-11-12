package com.pageturner.bookstore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_books_genre", columnList = "genre"),
        @Index(name = "idx_books_author", columnList = "author")
})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 80)
    private String genre;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    /** Optimistic locking: protects stock from lost updates under concurrent checkouts. */
    @Version
    private long version;

    protected Book() {
    }

    public Book(String isbn, String title, String author, String genre, String description, BigDecimal price, int stock) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    /** Domain rule lives on the entity: stock can never go negative. */
    public void decreaseStock(int quantity) {
        if (quantity > stock) {
            throw new IllegalStateException("Insufficient stock for '" + title + "'");
        }
        stock -= quantity;
    }

    public void increaseStock(int quantity) {
        stock += quantity;
    }

    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
