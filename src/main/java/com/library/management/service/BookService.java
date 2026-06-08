package com.library.management.service;

import com.library.management.dto.BookDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Author;
import com.library.management.model.Book;
import com.library.management.repository.AuthorRepository;
import com.library.management.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Cacheable(value = "books", key = "'all'")
    @Transactional(readOnly = true)
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "books", key = "#id")
    @Transactional(readOnly = true)
    public BookDTO getBookById(Long id) {
        return toDTO(findById(id));
    }

    @Cacheable(value = "books", key = "'available'")
    @Transactional(readOnly = true)
    public List<BookDTO> getAvailableBooks() {
        return bookRepository.findByAvailableCopiesGreaterThan(0).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookDTO> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookDTO> searchByGenre(String genre) {
        return bookRepository.findByGenreIgnoreCase(genre).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = "books", allEntries = true)
    public BookDTO createBook(BookDTO dto) {
        Author author = resolveAuthor(dto.getAuthorId());
        Book book = Book.builder()
            .title(dto.getTitle())
            .isbn(dto.getIsbn())
            .genre(dto.getGenre())
            .publishedYear(dto.getPublishedYear())
            .totalCopies(dto.getTotalCopies())
            .availableCopies(dto.getTotalCopies())
            .author(author)
            .build();
        return toDTO(bookRepository.save(book));
    }

    @CacheEvict(value = "books", allEntries = true)
    public BookDTO updateBook(Long id, BookDTO dto) {
        Book book = findById(id);
        Author author = resolveAuthor(dto.getAuthorId());
        book.setTitle(dto.getTitle());
        book.setIsbn(dto.getIsbn());
        book.setGenre(dto.getGenre());
        book.setPublishedYear(dto.getPublishedYear());
        book.setTotalCopies(dto.getTotalCopies());
        book.setAuthor(author);
        return toDTO(bookRepository.save(book));
    }

    @CacheEvict(value = "books", allEntries = true)
    public void deleteBook(Long id) {
        bookRepository.delete(findById(id));
    }

    Book findById(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private Author resolveAuthor(Long authorId) {
        return authorRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + authorId));
    }

    private BookDTO toDTO(Book book) {
        return BookDTO.builder()
            .id(book.getId())
            .title(book.getTitle())
            .isbn(book.getIsbn())
            .genre(book.getGenre())
            .publishedYear(book.getPublishedYear())
            .totalCopies(book.getTotalCopies())
            .availableCopies(book.getAvailableCopies())
            .authorId(book.getAuthor() != null ? book.getAuthor().getId() : null)
            .authorName(book.getAuthor() != null ? book.getAuthor().getName() : null)
            .build();
    }
}
