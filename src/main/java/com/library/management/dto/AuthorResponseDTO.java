package com.library.management.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorResponseDTO implements Serializable {

    private Long id;
    private String name;
    private String bio;
    private String nationality;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
