package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;
import com.project.airBnbApp.dto.GuestDTO;
import com.stripe.model.Event;

import java.util.List;

public interface BookingService {

    BookingDTO initializeBooking(BookingRequestDTO bookingRequest) ;

    BookingDTO addGuests(Long bookingId, List<GuestDTO> guestsDTOList);

    String initiatePayment(Long bookingId);

    void capturePayment(Event event);

    void cancelBooking(Long bookingId);

    List<BookingDTO> getAllBookingsInHotelById(Long hotelId);
}
