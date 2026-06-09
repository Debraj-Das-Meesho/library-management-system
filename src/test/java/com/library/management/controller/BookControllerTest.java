package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.BookDTO;
import com.library.management.dto.BookResponseDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@DisplayName("BookController web-layer tests")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    private BookResponseDTO bookResponse;

    @BeforeEach
    void setUp() {
        bookResponse = BookResponseDTO.builder()
            .id(1L)
            .title("1984")
            .isbn("978-0-452-28423-4")
            .genre("Dystopian Fiction")
            .publishedYear(1949)
            .totalCopies(5)
            .availableCopies(5)
            .authorId(1L)
            .authorName("George Orwell")
            .build();
    }

    // ─── GET /api/books ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/books → 200 with array of books")
    void getAllBooks_returns200WithBookList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of(bookResponse));

        mockMvc.perform(get("/api/books"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("1984"))
            .andExpect(jsonPath("$[0].authorName").value("George Orwell"));
    }

    @Test
    @DisplayName("GET /api/books → 200 with empty array when no books")
    void getAllBooks_returns200WithEmptyList_whenNoBooks() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of());

        mockMvc.perform(get("/api/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/books/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/books/{id} → 200 with book when found")
    void getBookById_returns200WhenFound() throws Exception {
        when(bookService.getBookById(1L)).thenReturn(bookResponse);

        mockMvc.perform(get("/api/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("1984"))
            .andExpect(jsonPath("$.isbn").value("978-0-452-28423-4"))
            .andExpect(jsonPath("$.availableCopies").value(5));
    }

    @Test
    @DisplayName("GET /api/books/{id} → 404 with error body when not found")
    void getBookById_returns404WhenNotFound() throws Exception {
        when(bookService.getBookById(99L))
            .thenThrow(new ResourceNotFoundException("Book not found with id: 99"));

        mockMvc.perform(get("/api/books/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Book not found with id: 99"));
    }

    // ─── GET /api/books/available ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/books/available → 200 with available books")
    void getAvailableBooks_returns200() throws Exception {
        when(bookService.getAvailableBooks()).thenReturn(List.of(bookResponse));

        mockMvc.perform(get("/api/books/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].availableCopies").value(5));
    }

    // ─── GET /api/books/search/* ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/books/search/title → 200 with matching books")
    void searchByTitle_returns200() throws Exception {
        when(bookService.searchByTitle("1984")).thenReturn(List.of(bookResponse));

        mockMvc.perform(get("/api/books/search/title").param("title", "1984"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("1984"));
    }

    @Test
    @DisplayName("GET /api/books/search/genre → 200 with genre-filtered books")
    void searchByGenre_returns200() throws Exception {
        when(bookService.searchByGenre("Dystopian Fiction")).thenReturn(List.of(bookResponse));

        mockMvc.perform(get("/api/books/search/genre").param("genre", "Dystopian Fiction"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].genre").value("Dystopian Fiction"));
    }

    // ─── POST /api/books ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/books → 201 with created book body")
    void createBook_returns201WithBody() throws Exception {
        BookDTO request = BookDTO.builder()
            .title("1984")
            .isbn("978-0-452-28423-4")
            .genre("Dystopian Fiction")
            .publishedYear(1949)
            .totalCopies(5)
            .authorId(1L)
            .build();
        when(bookService.createBook(any(BookDTO.class))).thenReturn(bookResponse);

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("1984"))
            .andExpect(jsonPath("$.authorName").value("George Orwell"));
    }

    @Test
    @DisplayName("POST /api/books → 400 when title is missing")
    void createBook_returns400OnMissingTitle() throws Exception {
        BookDTO invalid = BookDTO.builder()
            .isbn("978-0-452-28423-4")
            .totalCopies(5)
            .authorId(1L)
            .build();

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors[0]").value("Title is required"));
    }

    @Test
    @DisplayName("POST /api/books → 400 when totalCopies is 0")
    void createBook_returns400WhenTotalCopiesZero() throws Exception {
        BookDTO invalid = BookDTO.builder()
            .title("1984")
            .isbn("978-0-452-28423-4")
            .totalCopies(0)
            .authorId(1L)
            .build();

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /api/books → 400 when authorId is null")
    void createBook_returns400WhenAuthorIdNull() throws Exception {
        BookDTO invalid = BookDTO.builder()
            .title("1984")
            .isbn("978-0-452-28423-4")
            .totalCopies(5)
            .build();

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/books/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/books/{id} → 200 with updated book")
    void updateBook_returns200WithUpdatedBody() throws Exception {
        BookDTO request = BookDTO.builder()
            .title("1984 Special Edition")
            .isbn("978-0-452-28423-4")
            .genre("Dystopian Fiction")
            .publishedYear(1949)
            .totalCopies(10)
            .authorId(1L)
            .build();
        BookResponseDTO updated = BookResponseDTO.builder()
            .id(1L).title("1984 Special Edition").isbn("978-0-452-28423-4")
            .genre("Dystopian Fiction").publishedYear(1949).totalCopies(10)
            .availableCopies(10).authorId(1L).authorName("George Orwell")
            .build();
        when(bookService.updateBook(eq(1L), any(BookDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("1984 Special Edition"))
            .andExpect(jsonPath("$.totalCopies").value(10));
    }

    @Test
    @DisplayName("PUT /api/books/{id} → 404 when book does not exist")
    void updateBook_returns404WhenNotFound() throws Exception {
        BookDTO request = BookDTO.builder()
            .title("1984")
            .isbn("978-0-452-28423-4")
            .totalCopies(5)
            .authorId(1L)
            .build();
        when(bookService.updateBook(eq(99L), any(BookDTO.class)))
            .thenThrow(new ResourceNotFoundException("Book not found with id: 99"));

        mockMvc.perform(put("/api/books/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/books/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/books/{id} → 204 No Content")
    void deleteBook_returns204() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/1"))
            .andExpect(status().isNoContent());
        verify(bookService).deleteBook(1L);
    }

    @Test
    @DisplayName("DELETE /api/books/{id} → 404 when not found")
    void deleteBook_returns404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found with id: 99"))
            .when(bookService).deleteBook(99L);

        mockMvc.perform(delete("/api/books/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Book not found with id: 99"));
    }
}
