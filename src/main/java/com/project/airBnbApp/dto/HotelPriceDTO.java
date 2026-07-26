package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.Hotel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceDTO {

    private Hotel hotel;
    private Double price;
}
