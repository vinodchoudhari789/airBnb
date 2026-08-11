package com.project.airBnbApp.controller;

import com.project.airBnbApp.dto.RoomDTO;
import com.project.airBnbApp.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomsAdminController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDTO> createNewRoom(@PathVariable Long hotelId, @RequestBody RoomDTO roomDTO){
        RoomDTO createdRoomDTO = roomService.createNewRoom(hotelId, roomDTO);
        return new ResponseEntity<>(createdRoomDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAllRoomsInHotel(@PathVariable Long hotelId){
        return ResponseEntity.ok(roomService.getAllRoomsInHotel(hotelId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long hotelId, @PathVariable Long roomId){
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomDTO> updateRoomById(@PathVariable Long hotelId, @PathVariable Long roomId,
                                                  @RequestBody RoomDTO roomDTO){
        return ResponseEntity.ok(roomService.updateRoomById(hotelId, roomId, roomDTO));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<RoomDTO> deleteRoomById(@PathVariable Long hotelId, @PathVariable Long roomId){
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }

}

