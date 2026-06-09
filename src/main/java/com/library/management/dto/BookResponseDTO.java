package com.library.management.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponseDTO implements Serializable {

    private Long id;
    private String title;
    private String isbn;
    private String genre;
    private Integer publishedYear;
    private Integer totalCopies;
    private Integer availableCopies;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
