package com.pageturner.bookstore.ai;

import com.pageturner.bookstore.exception.AiUnavailableException;
import com.pageturner.bookstore.model.Book;
import com.pageturner.bookstore.repository.BookRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    /** Counts how many texts were sent for embedding. */
    static class CountingModel extends HashingEmbeddingModel {
        final AtomicInteger embedded = new AtomicInteger();

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            embedded.addAndGet(segments.size());
            return super.embedAll(segments);
        }
    }

    private static Book book(long id, String title, String desc, int stock) {
        Book b = new Book("ISBN-" + id, title, "Author", "Genre", desc, BigDecimal.TEN, stock);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    @Test
    void unchangedBooksAreEmbeddedOnlyOnce() {
        BookRepository repo = mock(BookRepository.class);
        when(repo.findAll()).thenReturn(List.of(book(1, "One", "alpha beta gamma", 1), book(2, "Two", "delta epsilon zeta", 1)));
        CountingModel model = new CountingModel();
        RecommendationService service = new RecommendationService(repo, model);

        service.byQuery("alpha", 5);
        assertEquals(3, model.embedded.get(), "2 books + 1 query on first call");
        service.byQuery("alpha", 5);
        assertEquals(4, model.embedded.get(), "only the query is embedded again");
    }

    @Test
    void embeddingFailureBecomesAiUnavailable() {
        BookRepository repo = mock(BookRepository.class);
        when(repo.findAll()).thenReturn(List.of(book(1, "One", "alpha", 1)));
        EmbeddingModel broken = segments -> {
            throw new RuntimeException("connection refused");
        };
        RecommendationService service = new RecommendationService(repo, broken);

        assertThrows(AiUnavailableException.class, () -> service.byQuery("alpha", 3));
    }

    @Test
    void cosineHandlesZeroVectors() {
        Embedding zero = Embedding.from(new float[]{0, 0});
        Embedding one = Embedding.from(new float[]{1, 0});
        assertEquals(0.0, RecommendationService.cosine(zero, one));
        assertEquals(1.0, RecommendationService.cosine(one, one), 1e-9);
    }
}
