package com.library.management.service;

import com.library.management.dto.AuthorDTO;
import com.library.management.dto.AuthorResponseDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Author;
import com.library.management.repository.AuthorRepository;
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
@DisplayName("AuthorService unit tests")
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author author;

    @BeforeEach
    void setUp() {
        author = Author.builder()
            .id(1L)
            .name("George Orwell")
            .bio("English novelist and essayist")
            .nationality("British")
            .build();
    }

    // ─── createAuthor ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createAuthor: saves entity and returns DTO with assigned ID")
    void createAuthor_savesAndReturnsDTO() {
        AuthorDTO dto = AuthorDTO.builder()
            .name("George Orwell")
            .bio("English novelist and essayist")
            .nationality("British")
            .build();
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        AuthorResponseDTO result = authorService.createAuthor(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("George Orwell");
        assertThat(result.getNationality()).isEqualTo("British");
        verify(authorRepository).save(any(Author.class));
    }

    // ─── getAuthorById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAuthorById: returns DTO when author exists")
    void getAuthorById_returnsDTO_whenFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        AuthorResponseDTO result = authorService.getAuthorById(1L);

        assertThat(result.getName()).isEqualTo("George Orwell");
        assertThat(result.getBio()).isEqualTo("English novelist and essayist");
    }

    @Test
    @DisplayName("getAuthorById: throws ResourceNotFoundException when not found")
    void getAuthorById_throwsResourceNotFoundException_whenNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getAuthorById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─── getAllAuthors ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllAuthors: returns list of all authors as DTOs")
    void getAllAuthors_returnsAllAuthors() {
        Author second = Author.builder().id(2L).name("J.K. Rowling").nationality("British").build();
        when(authorRepository.findAll()).thenReturn(List.of(author, second));

        List<AuthorResponseDTO> results = authorService.getAllAuthors();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(AuthorResponseDTO::getName)
            .containsExactlyInAnyOrder("George Orwell", "J.K. Rowling");
    }

    @Test
    @DisplayName("getAllAuthors: returns empty list when no authors exist")
    void getAllAuthors_returnsEmptyList_whenNoAuthors() {
        when(authorRepository.findAll()).thenReturn(List.of());

        List<AuthorResponseDTO> results = authorService.getAllAuthors();

        assertThat(results).isEmpty();
    }

    // ─── updateAuthor ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAuthor: mutates entity fields and persists")
    void updateAuthor_updatesFieldsAndSaves() {
        AuthorDTO dto = AuthorDTO.builder()
            .name("Eric Blair")
            .bio("Updated biography")
            .nationality("British")
            .build();
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        authorService.updateAuthor(1L, dto);

        assertThat(author.getName()).isEqualTo("Eric Blair");
        assertThat(author.getBio()).isEqualTo("Updated biography");
        verify(authorRepository).save(author);
    }

    @Test
    @DisplayName("updateAuthor: throws ResourceNotFoundException when not found")
    void updateAuthor_throwsWhenNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.updateAuthor(99L, new AuthorDTO()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── deleteAuthor ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAuthor: calls repository.delete with the found author")
    void deleteAuthor_deletesWhenFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        authorService.deleteAuthor(1L);

        verify(authorRepository).delete(author);
    }

    @Test
    @DisplayName("deleteAuthor: throws ResourceNotFoundException when not found")
    void deleteAuthor_throwsWhenNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.deleteAuthor(99L))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(authorRepository, never()).delete(any());
    }

    // ─── searchAuthors ──────────────────────────────────────────────────────

    @Test
    @DisplayName("searchAuthors: returns matching authors")
    void searchAuthors_returnsMatchingList() {
        when(authorRepository.findByNameContainingIgnoreCase("Orwell"))
            .thenReturn(List.of(author));

        List<AuthorResponseDTO> results = authorService.searchAuthors("Orwell");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("George Orwell");
    }

    @Test
    @DisplayName("searchAuthors: returns empty list when no match")
    void searchAuthors_returnsEmptyList_whenNoMatch() {
        when(authorRepository.findByNameContainingIgnoreCase("Tolkien"))
            .thenReturn(List.of());

        List<AuthorResponseDTO> results = authorService.searchAuthors("Tolkien");

        assertThat(results).isEmpty();
    }
}
