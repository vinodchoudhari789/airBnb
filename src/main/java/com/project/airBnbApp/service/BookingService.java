package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;
import com.project.airBnbApp.dto.GuestDTO;

import java.util.List;

public interface BookingService {

    BookingDTO initializeBooking(BookingRequestDTO bookingRequest) ;

    BookingDTO addGuests(Long bookingId, List<GuestDTO> guestsDTOList);
}
