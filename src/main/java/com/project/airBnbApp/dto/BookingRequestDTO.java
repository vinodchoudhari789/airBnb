package com.project.airBnbApp.dto;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequestDTO {

    private Long hotelId;

    private Long roomId;

    private Integer roomsCount;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

}
