package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.RoomDTO;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface RoomService {

    RoomDTO createNewRoom(Long hotelId, RoomDTO roomDTO);

    List<RoomDTO> getAllRoomsInHotel(Long hotelId);

    RoomDTO getRoomById(Long roomId);

    void deleteRoomById(Long roomId);

    RoomDTO updateRoomById(Long hotelId, Long roomId, RoomDTO roomDTO);
}
