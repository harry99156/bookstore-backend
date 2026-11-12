package com.pageturner.bookstore.service;

import com.pageturner.bookstore.dto.BookDtos.BookRequest;
import com.pageturner.bookstore.exception.BusinessRuleException;
import com.pageturner.bookstore.exception.ResourceNotFoundException;
import com.pageturner.bookstore.model.Book;
import com.pageturner.bookstore.repository.BookRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository books;

    public BookService(BookRepository books) {
        this.books = books;
    }

    /** All filters are optional and combine with AND; text filters are case-insensitive. */
    public Page<Book> search(String q, String genre, String author, BigDecimal minPrice, BigDecimal maxPrice,
                             Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (genre != null && !genre.isBlank()) {
                p.add(cb.equal(cb.lower(root.get("genre")), genre.toLowerCase()));
            }
            if (author != null && !author.isBlank()) {
                p.add(cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%"));
            }
            if (minPrice != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                p.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return books.findAll(spec, pageable);
    }

    public Book get(Long id) {
        return books.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book " + id + " not found"));
    }

    @Transactional
    public Book create(BookRequest r) {
        if (books.existsByIsbn(r.isbn())) {
            throw new BusinessRuleException("A book with ISBN " + r.isbn() + " already exists");
        }
        return books.save(new Book(r.isbn(), r.title(), r.author(), r.genre(), r.description(), r.price(), r.stock()));
    }

    @Transactional
    public Book update(Long id, BookRequest r) {
        Book b = get(id);
        if (!b.getIsbn().equals(r.isbn()) && books.existsByIsbn(r.isbn())) {
            throw new BusinessRuleException("A book with ISBN " + r.isbn() + " already exists");
        }
        b.setIsbn(r.isbn());
        b.setTitle(r.title());
        b.setAuthor(r.author());
        b.setGenre(r.genre());
        b.setDescription(r.description());
        b.setPrice(r.price());
        b.setStock(r.stock());
        return b;
    }

    @Transactional
    public void delete(Long id) {
        books.delete(get(id));
    }
}
