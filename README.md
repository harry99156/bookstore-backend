# PageTurner Bookstore Backend

Full-stack eCommerce bookstore in one deployable jar: a REST backend (catalog search, shopping cart, atomic checkout with stock control, order history and cancellation), AI-powered book recommendations, and a small storefront UI served at `/`.
Designed and built with an agentic IDE (IBM Bob).

## How IBM Bob was used

Bob acted as an agent, not an autocomplete: it planned the work with a todo list, generated the code layer by layer, ran `mvn verify`, read a real failure (`LazyInitializationException`) and fixed its root cause, then drove the running jar over HTTP and loaded the storefront in a headless browser to verify it. It also reused an embedding setup from another project for the AI recommendations and reviewed its own output (it caught a Javadoc that promised restocking the code did not yet do).

Full write-up with the design decisions and the reasons behind them, an architecture diagram, and the demo video script: [docs/DEVELOPMENT_WITH_BOB.md](docs/DEVELOPMENT_WITH_BOB.md).

**Stack:** Java 21, Spring Boot 3.3, Spring Data JPA, Bean Validation, springdoc OpenAPI, LangChain4j + Ollama (AI recommendations), H2 (dev/test) / PostgreSQL (prod), Docker, GitHub Actions.

## Run it

```bash
mvn spring-boot:run                       # H2 in-memory, seeded with 8 books
# or
docker compose up --build                 # API + PostgreSQL
```

- **Storefront:** http://localhost:8080/ - browse, filter, sort, an AI "describe what you want" tab, cart, checkout, and order lookup/cancel
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

```bash
mvn verify                                # 18 tests (API end-to-end, recommendations, storefront, config), no Ollama needed
```

## API (`/api/v1`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/books?q&genre&author&minPrice&maxPrice&page&size&sortBy&direction` | Search, filter, sort, paginate (size capped at 100) |
| GET / POST / PUT / DELETE | `/books`, `/books/{id}` | Catalog management (201 + `Location` on create) |
| POST | `/carts` | Create a cart |
| GET | `/carts/{cartId}` | View cart with line totals |
| POST | `/carts/{cartId}/items` | Add book (merges quantities, checks stock) |
| PUT / DELETE | `/carts/{cartId}/items/{bookId}` | Change quantity / remove |
| POST | `/orders` | Checkout: validates stock, decrements it, snapshots prices, clears the cart - one transaction |
| GET | `/orders/{id}`, `/orders?email=` | Order lookup / customer history |
| POST | `/orders/{id}/cancel` | Cancel and restock |
| GET | `/books/recommendations?query=...` or `?similarTo={bookId}&limit=` | **AI:** in-stock books ranked by meaning (embedding cosine similarity) |

### AI recommendations

Books are embedded (title, author, genre, description) with a local Ollama model (`nomic-embed-text`, via LangChain4j) and ranked against a free-text query or another book. No keywords need to match: *"a story of wizards, dragons and a quest"* returns *The Hobbit* first; *"how to write better software"* returns *Clean Code*.

- Embeddings are cached per book and keyed by a hash of its text, so edits are re-embedded and deleted books are dropped automatically, with no coupling to the catalog write path.
- Out-of-stock books are never recommended.
- If Ollama is down, only this endpoint returns `503`; the rest of the API is unaffected.
- `bookstore.ai.embeddings=local` switches to an offline keyword-hash embedder (used by the tests, no Ollama needed). It is keyword-level only, not semantic.

```bash
ollama pull nomic-embed-text
curl "localhost:8080/api/v1/books/recommendations?query=a%20story%20of%20wizards%20and%20dragons&limit=3"
curl "localhost:8080/api/v1/books/recommendations?similarTo=3"
```

Config: `bookstore.ai.base-url` (default `http://localhost:11434`), `bookstore.ai.embedding-model`, `bookstore.ai.embeddings` (`ollama` | `local`).

Errors use RFC 7807 `ProblemDetail`: `400` validation (with per-field `errors`), `404` not found, `409` business-rule conflicts (duplicate ISBN, insufficient stock, empty cart, double cancel, concurrent modification).

### Quick demo

```bash
CART=$(curl -s -X POST localhost:8080/api/v1/carts | jq -r .id)
curl -s -X POST localhost:8080/api/v1/carts/$CART/items -H 'Content-Type: application/json' -d '{"bookId":1,"quantity":2}'
curl -s -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -d "{\"cartId\":\"$CART\",\"customerName\":\"Ada\",\"customerEmail\":\"ada@example.com\",\"shippingAddress\":\"1 Main St\"}"
```

## Deploy to the cloud (Render)

`render.yaml` is a Blueprint that builds the `Dockerfile` and provisions a PostgreSQL database.

1. Push this repository to GitHub.
2. Render dashboard -> **New -> Blueprint** -> select the repository -> **Apply**.
3. When the build finishes, the storefront is at `https://pageturner-bookstore.onrender.com/` (the exact URL is shown in the dashboard).

Notes: the free tier sleeps when idle, so the first request after a pause is slow, and the free database has a limited lifetime. The AI tab needs an embedding server; Ollama on `localhost` does not exist in the cloud, so there it reports "AI recommendations are temporarily unavailable" while the rest of the shop works. Set `BOOKSTORE_AI_BASE_URL` to a reachable Ollama server to enable it, or `BOOKSTORE_AI_EMBEDDINGS=local` for the keyword-level fallback.

## Storefront

`src/main/resources/static/index.html` is a single dependency-free page (vanilla JS, no build step, no external requests) that talks only to the `/api/v1` endpoints above. The cart id is kept in `localStorage`; server errors (409 out of stock, 400 validation) are shown to the shopper; if the AI backend is down the AI tab says so and normal browsing keeps working. All dynamic text is inserted with `textContent`, so catalog data cannot inject HTML.

## Design decisions

- **Layered:** `web` (controllers, thin) -> `service` (transactions, rules) -> `repository` -> `model`. DTO records separate the API contract from JPA entities.
- **Stock integrity:** `Book.decreaseStock` enforces the invariant; `@Version` optimistic locking turns concurrent oversell into a `409` instead of a lost update.
- **Order history is immutable:** `OrderItem` snapshots title and unit price at purchase time.
- **Config via environment:** `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`; `postgres` profile for production.
- **Ops:** multi-stage Docker build, non-root user, container `HEALTHCHECK`, Actuator health probes, CI on every push/PR.

## Known limitations / next steps

- No authentication: catalog write endpoints and order lookup are open. Next step: Spring Security + JWT with an `ADMIN` role for catalog writes and per-customer order access.
- Payments are out of scope (checkout creates the order only).
- `ddl-auto=update` is convenient for a demo; production should use Flyway migrations.
