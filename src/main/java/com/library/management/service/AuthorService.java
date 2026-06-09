package com.library.management.service;

import com.library.management.dto.AuthorDTO;
import com.library.management.dto.AuthorResponseDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Author;
import com.library.management.repository.AuthorRepository;
import com.library.management.utils.constants.AppConstants;
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

    @Cacheable(value = AppConstants.CACHE_AUTHORS, key = AppConstants.CACHE_KEY_ALL)
    @Transactional(readOnly = true)
    public List<AuthorResponseDTO> getAllAuthors() {
        return authorRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Cacheable(value = AppConstants.CACHE_AUTHORS, key = "#id")
    @Transactional(readOnly = true)
    public AuthorResponseDTO getAuthorById(Long id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public List<AuthorResponseDTO> searchAuthors(String name) {
        return authorRepository.findByNameContainingIgnoreCase(name).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = AppConstants.CACHE_AUTHORS, allEntries = true)
    public AuthorResponseDTO createAuthor(AuthorDTO dto) {
        Author author = Author.builder()
            .name(dto.getName())
            .bio(dto.getBio())
            .nationality(dto.getNationality())
            .build();
        return toDTO(authorRepository.save(author));
    }

    @CacheEvict(value = AppConstants.CACHE_AUTHORS, allEntries = true)
    public AuthorResponseDTO updateAuthor(Long id, AuthorDTO dto) {
        Author author = findById(id);
        author.setName(dto.getName());
        author.setBio(dto.getBio());
        author.setNationality(dto.getNationality());
        return toDTO(authorRepository.save(author));
    }

    @CacheEvict(value = AppConstants.CACHE_AUTHORS, allEntries = true)
    public void deleteAuthor(Long id) {
        authorRepository.delete(findById(id));
    }

    Author findById(Long id) {
        return authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
    }

    private AuthorResponseDTO toDTO(Author author) {
        return AuthorResponseDTO.builder()
            .id(author.getId())
            .name(author.getName())
            .bio(author.getBio())
            .nationality(author.getNationality())
            .createdAt(author.getCreatedAt())
            .updatedAt(author.getUpdatedAt())
            .build();
    }
}
