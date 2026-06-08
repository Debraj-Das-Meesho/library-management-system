package com.library.management.service;

import com.library.management.dto.AuthorDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Author;
import com.library.management.repository.AuthorRepository;
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
public class AuthorService {

    private final AuthorRepository authorRepository;

    @Cacheable(value = "authors", key = "'all'")
    @Transactional(readOnly = true)
    public List<AuthorDTO> getAllAuthors() {
        return authorRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "authors", key = "#id")
    @Transactional(readOnly = true)
    public AuthorDTO getAuthorById(Long id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public List<AuthorDTO> searchAuthors(String name) {
        return authorRepository.findByNameContainingIgnoreCase(name).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = "authors", allEntries = true)
    public AuthorDTO createAuthor(AuthorDTO dto) {
        Author author = Author.builder()
            .name(dto.getName())
            .bio(dto.getBio())
            .nationality(dto.getNationality())
            .build();
        return toDTO(authorRepository.save(author));
    }

    @CacheEvict(value = "authors", allEntries = true)
    public AuthorDTO updateAuthor(Long id, AuthorDTO dto) {
        Author author = findById(id);
        author.setName(dto.getName());
        author.setBio(dto.getBio());
        author.setNationality(dto.getNationality());
        return toDTO(authorRepository.save(author));
    }

    @CacheEvict(value = "authors", allEntries = true)
    public void deleteAuthor(Long id) {
        authorRepository.delete(findById(id));
    }

    Author findById(Long id) {
        return authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
    }

    private AuthorDTO toDTO(Author author) {
        return AuthorDTO.builder()
            .id(author.getId())
            .name(author.getName())
            .bio(author.getBio())
            .nationality(author.getNationality())
            .build();
    }
}
