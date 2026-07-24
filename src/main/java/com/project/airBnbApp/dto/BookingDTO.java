package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.Set;

public class BookingDTO {

    private Long id;

    private Integer roomsCount;

    private LocalDateTime checkInDate;

    private LocalDateTime checkOutDate;

    private BookingStatus bookingStatus;

    private Set<GuestDTO> guestSet;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
