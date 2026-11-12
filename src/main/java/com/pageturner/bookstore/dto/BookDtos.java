package com.pageturner.bookstore.dto;

import com.pageturner.bookstore.model.Book;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class BookDtos {

    private BookDtos() {
    }

    public record BookRequest(
            @NotBlank @Size(max = 20) String isbn,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 255) String author,
            @NotBlank @Size(max = 80) String genre,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @Min(0) int stock) {
    }

    public record BookResponse(Long id, String isbn, String title, String author, String genre,
                               String description, BigDecimal price, int stock) {
        public static BookResponse from(Book b) {
            return new BookResponse(b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getGenre(),
                    b.getDescription(), b.getPrice(), b.getStock());
        }
    }
}
