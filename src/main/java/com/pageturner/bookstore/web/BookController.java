package com.pageturner.bookstore.web;

import com.pageturner.bookstore.dto.BookDtos.BookRequest;
import com.pageturner.bookstore.dto.BookDtos.BookResponse;
import com.pageturner.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Catalog browsing, search and management")
public class BookController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @Operation(summary = "Search the catalog (paged, filterable, sortable)")
    @GetMapping
    public Page<BookResponse> search(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String genre,
                                     @RequestParam(required = false) String author,
                                     @RequestParam(required = false) BigDecimal minPrice,
                                     @RequestParam(required = false) BigDecimal maxPrice,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(defaultValue = "title") String sortBy,
                                     @RequestParam(defaultValue = "asc") String direction) {
        String field = switch (sortBy) {
            case "price", "author", "title", "genre" -> sortBy;
            default -> "title";
        };
        Sort sort = Sort.by("desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
        return service.search(q, genre, author, minPrice, maxPrice, pr).map(BookResponse::from);
    }

    @Operation(summary = "Get a book by id")
    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return BookResponse.from(service.get(id));
    }

    @Operation(summary = "Add a book to the catalog")
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        BookResponse body = BookResponse.from(service.create(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @Operation(summary = "Update a book")
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return BookResponse.from(service.update(id, request));
    }

    @Operation(summary = "Delete a book")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
