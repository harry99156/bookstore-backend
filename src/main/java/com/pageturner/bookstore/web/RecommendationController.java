package com.pageturner.bookstore.web;

import com.pageturner.bookstore.ai.RecommendationService;
import com.pageturner.bookstore.dto.BookDtos.BookResponse;
import com.pageturner.bookstore.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books/recommendations")
@Tag(name = "AI Recommendations", description = "Semantic book discovery powered by embeddings")
public class RecommendationController {

    private static final int MAX_LIMIT = 20;

    public record Recommendation(BookResponse book, double score) {
    }

    public record RecommendationResponse(String basis, List<Recommendation> results) {
    }

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @Operation(summary = "Recommend in-stock books by meaning",
            description = "Pass either 'query' (free text, e.g. \"epic science fiction about politics and desert planets\") "
                    + "or 'similarTo' (a book id). Results are ranked by embedding cosine similarity.")
    @GetMapping
    public RecommendationResponse recommend(@RequestParam(required = false) String query,
                                            @RequestParam(required = false) Long similarTo,
                                            @RequestParam(defaultValue = "5") int limit) {
        boolean hasQuery = query != null && !query.isBlank();
        if (hasQuery == (similarTo != null)) {
            throw new InvalidRequestException("Provide exactly one of 'query' or 'similarTo'");
        }
        int n = Math.min(Math.max(limit, 1), MAX_LIMIT);
        var scored = hasQuery ? service.byQuery(query, n) : service.similarTo(similarTo, n);
        return new RecommendationResponse(
                hasQuery ? "query: " + query : "similar to book " + similarTo,
                scored.stream().map(s -> new Recommendation(BookResponse.from(s.book()), s.score())).toList());
    }
}
