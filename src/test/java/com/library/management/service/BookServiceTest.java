package com.library.management.service;

import com.library.management.dto.BookDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Author;
import com.library.management.model.Book;
import com.library.management.repository.AuthorRepository;
import com.library.management.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService unit tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    private Author author;
    private Book book;
    private BookDTO bookDTO;

    @BeforeEach
    void setUp() {
        author = Author.builder()
            .id(1L)
            .name("George Orwell")
            .nationality("British")
            .build();

        book = Book.builder()
            .id(1L)
            .title("1984")
            .isbn("978-0-452-28423-4")
            .genre("Dystopian Fiction")
            .publishedYear(1949)
            .totalCopies(5)
            .availableCopies(5)
            .author(author)
            .build();

        bookDTO = BookDTO.builder()
            .title("1984")
            .isbn("978-0-452-28423-4")
            .genre("Dystopian Fiction")
            .publishedYear(1949)
            .totalCopies(5)
            .authorId(1L)
            .build();
    }

    // ─── createBook ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBook: saves book with resolved author, availableCopies equals totalCopies")
    void createBook_savesBookWithAuthorAndSetsAvailableCopies() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        BookDTO result = bookService.createBook(bookDTO);

        assertThat(result.getTitle()).isEqualTo("1984");
        assertThat(result.getAuthorName()).isEqualTo("George Orwell");
        assertThat(result.getAvailableCopies()).isEqualTo(5);
        verify(bookRepository).save(argThat(b ->
            b.getAvailableCopies().equals(b.getTotalCopies())
        ));
    }

    @Test
    @DisplayName("createBook: throws ResourceNotFoundException when author not found")
    void createBook_throwsWhenAuthorNotFound() {
        bookDTO.setAuthorId(99L);
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.createBook(bookDTO))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
        verify(bookRepository, never()).save(any());
    }

    // ─── getBookById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBookById: returns DTO when book exists")
    void getBookById_returnsDTO_whenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookDTO result = bookService.getBookById(1L);

        assertThat(result.getTitle()).isEqualTo("1984");
        assertThat(result.getIsbn()).isEqualTo("978-0-452-28423-4");
    }

    @Test
    @DisplayName("getBookById: throws ResourceNotFoundException when not found")
    void getBookById_throwsWhenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─── getAllBooks ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllBooks: returns list of all books")
    void getAllBooks_returnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(book));

        List<BookDTO> results = bookService.getAllBooks();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("1984");
    }

    // ─── getAvailableBooks ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAvailableBooks: returns only books with availableCopies > 0")
    void getAvailableBooks_returnsOnlyBooksWithCopies() {
        when(bookRepository.findByAvailableCopiesGreaterThan(0)).thenReturn(List.of(book));

        List<BookDTO> results = bookService.getAvailableBooks();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAvailableCopies()).isGreaterThan(0);
    }

    // ─── updateBook ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBook: mutates entity fields and persists")
    void updateBook_updatesFieldsCorrectly() {
        BookDTO update = BookDTO.builder()
            .title("1984 Special Edition")
            .isbn("978-0-452-28423-4")
            .genre("Classic Fiction")
            .publishedYear(1949)
            .totalCopies(10)
            .authorId(1L)
            .build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.updateBook(1L, update);

        assertThat(book.getTitle()).isEqualTo("1984 Special Edition");
        assertThat(book.getGenre()).isEqualTo("Classic Fiction");
        assertThat(book.getTotalCopies()).isEqualTo(10);
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("updateBook: throws when book not found")
    void updateBook_throwsWhenBookNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(99L, bookDTO))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── deleteBook ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBook: calls repository.delete with found book")
    void deleteBook_deletesWhenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.deleteBook(1L);

        verify(bookRepository).delete(book);
    }

    @Test
    @DisplayName("deleteBook: throws when not found, never calls delete")
    void deleteBook_throwsWhenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(99L))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(bookRepository, never()).delete(any());
    }

    // ─── search ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchByTitle: returns case-insensitive title matches")
    void searchByTitle_returnsMatchingBooks() {
        when(bookRepository.findByTitleContainingIgnoreCase("1984"))
            .thenReturn(List.of(book));

        List<BookDTO> results = bookService.searchByTitle("1984");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("1984");
    }

    @Test
    @DisplayName("searchByGenre: returns genre-filtered books")
    void searchByGenre_returnsMatchingBooks() {
        when(bookRepository.findByGenreIgnoreCase("Dystopian Fiction"))
            .thenReturn(List.of(book));

        List<BookDTO> results = bookService.searchByGenre("Dystopian Fiction");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGenre()).isEqualTo("Dystopian Fiction");
    }
}
