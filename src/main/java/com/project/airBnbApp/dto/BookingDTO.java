package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.enums.BookingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class BookingDTO {

    private Long id;

    private Integer roomsCount;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private BookingStatus bookingStatus;

    private Set<GuestDTO> guestSet;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
