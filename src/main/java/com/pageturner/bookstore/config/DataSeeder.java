package com.pageturner.bookstore.config;

import com.pageturner.bookstore.model.Book;
import com.pageturner.bookstore.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

/** Seeds a small catalog on first start (skipped under the "test" profile and when data already exists). */
@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    CommandLineRunner seedBooks(BookRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.saveAll(List.of(
                    new Book("9780132350884", "Clean Code", "Robert C. Martin", "Technology",
                            "A handbook of agile software craftsmanship.", new BigDecimal("39.99"), 25),
                    new Book("9780134685991", "Effective Java", "Joshua Bloch", "Technology",
                            "Best practices for the Java platform.", new BigDecimal("45.50"), 18),
                    new Book("9780441013593", "Dune", "Frank Herbert", "Science Fiction",
                            "Epic tale of politics, religion and ecology on the desert planet Arrakis.", new BigDecimal("14.99"), 40),
                    new Book("9780547928227", "The Hobbit", "J.R.R. Tolkien", "Fantasy",
                            "Bilbo Baggins joins a company of dwarves on a quest for treasure.", new BigDecimal("12.99"), 35),
                    new Book("9780743273565", "The Great Gatsby", "F. Scott Fitzgerald", "Classics",
                            "A portrait of the Jazz Age and the American Dream.", new BigDecimal("9.99"), 50),
                    new Book("9780062316097", "Sapiens", "Yuval Noah Harari", "History",
                            "A brief history of humankind.", new BigDecimal("18.00"), 30),
                    new Book("9780735211292", "Atomic Habits", "James Clear", "Self-Help",
                            "Tiny changes, remarkable results.", new BigDecimal("16.20"), 3),
                    new Book("9781491950296", "Building Microservices", "Sam Newman", "Technology",
                            "Designing fine-grained systems.", new BigDecimal("52.00"), 12)));
        };
    }
}
