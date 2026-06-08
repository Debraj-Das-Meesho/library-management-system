package com.library.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDTO implements Serializable {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    private String genre;

    private Integer publishedYear;

    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Must have at least 1 copy")
    private Integer totalCopies;

    private Integer availableCopies;

    @NotNull(message = "Author ID is required")
    private Long authorId;

    private String authorName;
}
