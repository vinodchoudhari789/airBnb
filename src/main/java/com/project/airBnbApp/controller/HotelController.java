package com.project.airBnbApp.controller;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelDTO> createNewHotel(@RequestBody HotelDTO hotelDTO){
        log.info("Attempting to create a new hotel with name : {}",hotelDTO.getName());
        HotelDTO hotel = hotelService.createNewHotel(hotelDTO);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDTO> getHotelById(@PathVariable Long hotelId){
        HotelDTO hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDTO> updateHotelById(@PathVariable Long hotelId, @RequestBody HotelDTO hotelDTO){
        HotelDTO hotelDto = hotelService.updateHotelById(hotelId, hotelDTO);
        return ResponseEntity.ok(hotelDto);
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId){
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{hotelId}")
    public  ResponseEntity<Void> activateHotelById(@PathVariable Long hotelId){
        hotelService.activateHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<HotelDTO>> getAllHotels(){
        List<HotelDTO> hotelList = hotelService.getAllHotels();
        return ResponseEntity.ok(hotelList);
    }

}
