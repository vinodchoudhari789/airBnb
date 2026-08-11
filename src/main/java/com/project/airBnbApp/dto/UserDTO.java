package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private LocalDate dateOfBirth;
    private Gender gender;
}
