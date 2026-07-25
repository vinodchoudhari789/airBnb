package com.project.airBnbApp.controller;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;
import com.project.airBnbApp.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@Slf4j
@RequiredArgsConstructor
public class HotelBookingController {

    private final BookingService bookingService;

    @PostMapping("/init")
    public ResponseEntity<BookingDTO> initializeBooking(@RequestBody BookingRequestDTO bookingRequest){
        return ResponseEntity.ok(bookingService.initializeBooking(bookingRequest));
    }
}
