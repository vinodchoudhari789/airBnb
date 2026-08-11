package com.project.airBnbApp.controller;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.ProfileUpdateRequestDTO;
import com.project.airBnbApp.dto.UserDTO;
import com.project.airBnbApp.service.BookingService;
import com.project.airBnbApp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody ProfileUpdateRequestDTO profileUpdateRequestDTO){
        userService.updateProfile(profileUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myBookings")
    public ResponseEntity<List<BookingDTO>> getMyBookings(){
        return ResponseEntity.ok(bookingService.getMyBookings());
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getMyProfile(){
        return ResponseEntity.ok(userService.getMyProfile());
    }
}
