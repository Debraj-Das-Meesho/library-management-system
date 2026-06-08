# Library Management System API

A RESTful API built with **Java Spring Boot 3**, **PostgreSQL**, and **Redis** for managing books, authors, library members, and borrow/return workflows — fully Dockerized.

---

## Features

- **Books** — CRUD, search by title/genre, availability tracking
- **Authors** — CRUD, search by name
- **Members** — CRUD, membership lifecycle, deactivation
- **Borrow / Return** — inventory management, overdue detection, fine calculation ($0.50/day)
- **Redis Caching** — first retrieval hits PostgreSQL; subsequent requests served from Redis (10-min TTL, auto-evicted on writes)
- **Swagger UI** — interactive API documentation at `/swagger-ui.html`
- **Validation** — request-level bean validation with structured error responses
- **Dockerized** — one command brings up the full stack

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9 |
| Containers | Docker + Docker Compose |

---

## Architecture

```
┌──────────────────────────────────────────────────┐
│                   Client / Swagger                │
└───────────────────────┬──────────────────────────┘
                        │ HTTP
┌───────────────────────▼──────────────────────────┐
│              Spring Boot App (:8080)              │
│                                                   │
│  Controllers → Services → Repositories            │
│                    │                              │
│           @Cacheable / @CacheEvict                │
└──────────┬────────────────────────┬──────────────┘
           │                        │
┌──────────▼──────────┐  ┌──────────▼──────────────┐
│   PostgreSQL :5432  │  │     Redis :6379          │
│   (primary store)   │  │  (cache, 10-min TTL)     │
└─────────────────────┘  └─────────────────────────┘
```

**Caching flow:**
```
GET /api/books/{id}
  ├── Cache HIT  → return from Redis (fast)
  └── Cache MISS → query PostgreSQL → store in Redis → return

POST/PUT/DELETE /api/books
  └── @CacheEvict(allEntries=true) → clears stale cache entries
```

---

## Project Structure

```
library_management/
├── Dockerfile                          # Multi-stage Maven → JRE build
├── docker-compose.yml                  # postgres + redis + app services
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/library/management/
    │   │   ├── LibraryManagementApplication.java
    │   │   ├── config/
    │   │   │   └── RedisConfig.java            # JSON serializer, TTL config
    │   │   ├── controller/
    │   │   │   ├── AuthorController.java
    │   │   │   ├── BookController.java
    │   │   │   ├── MemberController.java
    │   │   │   └── BorrowController.java
    │   │   ├── service/                        # Business logic + caching
    │   │   │   ├── AuthorService.java
    │   │   │   ├── BookService.java
    │   │   │   ├── MemberService.java
    │   │   │   └── BorrowService.java
    │   │   ├── repository/                     # Spring Data JPA interfaces
    │   │   │   ├── AuthorRepository.java
    │   │   │   ├── BookRepository.java
    │   │   │   ├── MemberRepository.java
    │   │   │   └── BorrowRecordRepository.java
    │   │   ├── model/                          # JPA entities
    │   │   │   ├── Author.java
    │   │   │   ├── Book.java
    │   │   │   ├── Member.java
    │   │   │   ├── BorrowRecord.java
    │   │   │   └── BorrowStatus.java           # Enum: BORROWED | RETURNED | OVERDUE
    │   │   ├── dto/                            # Request / response objects
    │   │   │   ├── AuthorDTO.java
    │   │   │   ├── BookDTO.java
    │   │   │   ├── MemberDTO.java
    │   │   │   ├── BorrowRequestDTO.java
    │   │   │   └── BorrowResponseDTO.java
    │   │   └── exception/
    │   │       ├── GlobalExceptionHandler.java
    │   │       ├── ResourceNotFoundException.java
    │   │       ├── BookNotAvailableException.java
    │   │       └── ErrorResponse.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/library/management/
            ├── LibraryManagementApplicationTests.java
            ├── service/
            │   ├── AuthorServiceTest.java
            │   ├── BookServiceTest.java
            │   ├── MemberServiceTest.java
            │   └── BorrowServiceTest.java
            └── controller/
                ├── BookControllerTest.java
                └── BorrowControllerTest.java
```

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) or [Colima](https://github.com/abiosoft/colima)
- Docker Compose
- (Optional, for local dev) Java 17 + Maven 3.9 + PostgreSQL + Redis

---

## Getting Started

### Run with Docker (Recommended)

```bash
# 1. Clone / enter the project
cd library_management

# 2. Build the image and start all 3 services (postgres, redis, app)
docker-compose up --build

# The app is ready when you see:
#   Started LibraryManagementApplication in X.XXX seconds

# 3. Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

To run in the background:
```bash
docker-compose up --build -d
docker-compose logs -f app       # tail app logs
```

To stop and clean up:
```bash
docker-compose down              # stop containers, keep volumes
docker-compose down -v           # stop containers + delete DB/Redis data
```

### Run Locally (without Docker)

Requires a running PostgreSQL and Redis instance.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/librarydb
export SPRING_DATASOURCE_USERNAME=library_user
export SPRING_DATASOURCE_PASSWORD=library_pass
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379

mvn spring-boot:run
```

---

## API Endpoints

### Authors — `/api/authors`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/authors` | Get all authors *(cached)* |
| `GET` | `/api/authors/{id}` | Get author by ID *(cached)* |
| `GET` | `/api/authors/search?name=` | Search authors by name |
| `POST` | `/api/authors` | Create a new author |
| `PUT` | `/api/authors/{id}` | Update an author |
| `DELETE` | `/api/authors/{id}` | Delete an author |

### Books — `/api/books`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/books` | Get all books *(cached)* |
| `GET` | `/api/books/{id}` | Get book by ID *(cached)* |
| `GET` | `/api/books/available` | Get books with copies available *(cached)* |
| `GET` | `/api/books/search/title?title=` | Search by title (partial, case-insensitive) |
| `GET` | `/api/books/search/genre?genre=` | Search by genre |
| `POST` | `/api/books` | Create a new book |
| `PUT` | `/api/books/{id}` | Update a book |
| `DELETE` | `/api/books/{id}` | Delete a book |

### Members — `/api/members`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/members` | Get all members *(cached)* |
| `GET` | `/api/members/{id}` | Get member by ID *(cached)* |
| `GET` | `/api/members/search?name=` | Search members by name |
| `POST` | `/api/members` | Register a new member |
| `PUT` | `/api/members/{id}` | Update member details |
| `PATCH` | `/api/members/{id}/deactivate` | Deactivate a member |
| `DELETE` | `/api/members/{id}` | Delete a member |

### Borrows — `/api/borrows`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/borrows` | Borrow a book |
| `PUT` | `/api/borrows/{id}/return` | Return a borrowed book (fine auto-calculated) |
| `GET` | `/api/borrows/active` | All currently borrowed books |
| `GET` | `/api/borrows/overdue` | All overdue borrows (auto-marks status) |
| `GET` | `/api/borrows/member/{memberId}` | Borrow history for a member |
| `GET` | `/api/borrows/book/{bookId}` | Borrow history for a book |

---

## Request / Response Examples

### Create Author
```bash
curl -X POST http://localhost:8080/api/authors \
  -H "Content-Type: application/json" \
  -d '{"name":"George Orwell","bio":"English novelist","nationality":"British"}'
```
```json
{ "id": 1, "name": "George Orwell", "bio": "English novelist", "nationality": "British" }
```

### Create Book
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"1984","isbn":"978-0-452-28423-4","genre":"Dystopian Fiction","publishedYear":1949,"totalCopies":5,"authorId":1}'
```
```json
{ "id": 1, "title": "1984", "isbn": "978-0-452-28423-4", "genre": "Dystopian Fiction",
  "publishedYear": 1949, "totalCopies": 5, "availableCopies": 5,
  "authorId": 1, "authorName": "George Orwell" }
```

### Borrow a Book
```bash
curl -X POST http://localhost:8080/api/borrows \
  -H "Content-Type: application/json" \
  -d '{"bookId":1,"memberId":1,"borrowDays":14}'
```
```json
{ "id": 1, "bookId": 1, "bookTitle": "1984", "memberId": 1, "memberName": "Alice",
  "borrowDate": "2026-06-08", "dueDate": "2026-06-22",
  "returnDate": null, "fineAmount": 0.0, "status": "BORROWED" }
```

### Return a Book
```bash
curl -X PUT http://localhost:8080/api/borrows/1/return
```
```json
{ "id": 1, "status": "RETURNED", "returnDate": "2026-06-08", "fineAmount": 0.0 }
```

### Error Response (404)
```json
{ "status": 404, "message": "Book not found with id: 99", "timestamp": "...", "errors": null }
```

### Validation Error (400)
```json
{ "status": 400, "message": "Validation failed", "timestamp": "...", "errors": ["Title is required"] }
```

---

## Redis Caching

| Cache Name | Keys | TTL | Eviction Trigger |
|---|---|---|---|
| `books` | `all`, `available`, `{id}` | 10 min | Any book create/update/delete or borrow/return |
| `authors` | `all`, `{id}` | 10 min | Any author create/update/delete |
| `members` | `all`, `{id}` | 10 min | Any member create/update/delete |

Values are stored as typed JSON:
```json
{ "@class": "com.library.management.dto.BookDTO", "id": 1, "title": "1984", ... }
```

Inspect live cache:
```bash
docker exec library_redis redis-cli KEYS "*"
docker exec library_redis redis-cli TTL "books::all"
docker exec library_redis redis-cli GET "books::1"
```

---

## Running Tests

Tests are split into two layers — no running database or Redis required.

```bash
mvn test
```

| Test class | Type | What it tests |
|---|---|---|
| `AuthorServiceTest` | Unit (Mockito) | Service logic, CRUD, exception throwing |
| `BookServiceTest` | Unit (Mockito) | Service logic, author resolution, cache key logic |
| `MemberServiceTest` | Unit (Mockito) | Membership date auto-set, deactivation |
| `BorrowServiceTest` | Unit (Mockito) | Borrow/return flow, fine calculation, error cases |
| `BookControllerTest` | Web (`@WebMvcTest`) | HTTP status codes, JSON responses, validation |
| `BorrowControllerTest` | Web (`@WebMvcTest`) | Borrow/return HTTP flows, conflict errors |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/librarydb` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `library_user` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `library_pass` | DB password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis hostname |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |

---

## Swagger UI

After starting the app, open:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:
```
http://localhost:8080/api-docs
```

---

## Business Rules

- A book can be borrowed only if `availableCopies > 0`
- A member must be `active = true` to borrow
- Default borrow period is **14 days** (overridable via `borrowDays` field)
- Late return fine: **$0.50 per day** after the due date
- Returning an already-returned book returns `409 Conflict`
- Schema is auto-managed by Hibernate (`ddl-auto: update`) — no manual migrations needed
