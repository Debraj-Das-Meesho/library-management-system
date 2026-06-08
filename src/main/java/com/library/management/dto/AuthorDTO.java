package com.library.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorDTO implements Serializable {

    private Long id;

    @NotBlank(message = "Author name is required")
    private String name;

    private String bio;

    private String nationality;
}
