package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.Gender;
import lombok.Data;

@Data
public class GuestDTO {

    private Long id;

    private User user;

    private String name;

    private Integer age;

    private Gender gender;

}
