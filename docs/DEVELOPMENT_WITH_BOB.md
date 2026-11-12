# How this was built with IBM Bob (and a ~3 minute video script)

## Workflow

1. **Understand the environment** - Bob inspected the workspace and toolchain (Java 21, Maven 3.9) and reused the Spring Boot conventions of an existing project instead of guessing.
2. **Plan with a todo list** - Bob broke the work into scaffold -> domain -> services -> API -> ops -> tests -> verify, and tracked progress as it went.
3. **Generate in layers** - entities, repositories, DTO records, services, controllers, error handling, OpenAPI config, seed data, Dockerfile, compose, CI.
4. **Verify, don't assume** - Bob ran `mvn verify`, read the failure (`LazyInitializationException` serialising the cart outside a session), fixed the fetch strategy, and re-ran until 7/7 tests passed.
5. **Run the real artifact** - started the packaged jar and exercised the API over HTTP (browse, cart, over-stock 409, checkout, stock decrement).
6. **Add the AI capability** - Bob reused the embedding setup from an existing LangChain4j/Ollama project and built `GET /books/recommendations`: semantic search by free text or "similar to this book". It chose an embedding cache keyed by text hash (edits and deletes stay correct without touching the catalog code), a 503 that isolates AI failures from the rest of the API, and an offline embedder so tests run without Ollama. It then checked real results against the local Ollama (e.g. "wizards, dragons and a quest" returns The Hobbit first).
7. **Add a storefront** - to make it full stack, Bob wrote a single static page served by the same jar (no build tooling) that exercises every endpoint, including the AI tab. It then loaded the page in headless Edge against the running app and confirmed it rendered the live catalog (8 books, genre filter populated, cart created).
8. **Self-review** - caught and fixed its own issues: cancel Javadoc promised restocking that the code did not do yet; removed unused imports and a dead test line.

## Video outline (about 3:00)

| Time | Show | Say |
|---|---|---|
| 0:00-0:20 | Bob chat with the prompt | Goal: bookstore backend; I use Bob as an agent that plans, writes, runs and fixes. |
| 0:20-0:50 | Project tree + todo list | Layered design; todo list drives the work. |
| 0:50-1:30 | `BookController`, `OrderService.checkout`, `Book` with `@Version` | API design; transactional checkout; optimistic locking against overselling. |
| 1:30-2:00 | Failing `mvn verify` then green | Bob read the stack trace, fixed lazy loading, re-ran until green (now 15 tests). |
| 2:00-2:30 | Storefront at `/`: add to cart, checkout, see stock drop; try to over-order for the 409 message | Full stack: one jar serves the UI and the API. |
| 2:30-2:50 | Storefront AI tab: "a story of wizards, dragons and a quest" | Applied AI: embeddings rank books by meaning; Ollama down gives 503 for this endpoint only. |
| 2:50-3:00 | Dockerfile, compose, CI, README limitations | Deployable; honest next steps (auth, Flyway). |
