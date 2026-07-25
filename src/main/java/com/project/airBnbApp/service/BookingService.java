package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;

public interface BookingService {

    BookingDTO initializeBooking(BookingRequestDTO bookingRequest) ;

}
