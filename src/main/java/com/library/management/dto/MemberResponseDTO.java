package com.library.management.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDTO implements Serializable {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate membershipDate;
    private Boolean active;
}
