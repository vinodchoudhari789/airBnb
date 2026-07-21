package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.HotelContactInfo;
import lombok.Data;

@Data
public class HotelDTO {
    private Long id;

    private String name;

    private String city;

    private String[] photos;

    private String[] amenities;

    private HotelContactInfo contactInfo;

    private Boolean active;
}
