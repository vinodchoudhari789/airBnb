package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.RoomDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{

    private final ModelMapper modelMapper;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;


    @Override
    public RoomDTO createNewRoom(Long hotelId, RoomDTO roomDTO) {
        log.info("Starting creation of new room...");
        Room createdRoom = modelMapper.map(roomDTO, Room.class);

        log.info("Finding the hotel with ID : {}", hotelId);
        Hotel hotel =  hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        log.info("Fetched the hotel with ID : {}", hotelId);

        createdRoom.setHotel(hotel);
        roomRepository.save(createdRoom);
        // TODO: Create Inventory as soon as room  is created and if hotel is active

        log.info("Finished creation of new room successfully!!!");
        return modelMapper.map(createdRoom, RoomDTO.class);
    }

    @Override
    public List<RoomDTO> getAllRoomsInHotel(Long hotelId) {
        log.info("Fetching all rooms for hotel with ID : {}",hotelId);

        log.info("Finding the hotel with ID : {}", hotelId);
        Hotel hotel =  hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        log.info("Fetched the hotel with ID : {}", hotelId);

        List<RoomDTO> roomListDTO = hotel.getRoomList()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDTO.class))
                .toList();
        log.info("Fetched all rooms for hotel with ID : {}",hotelId);
        return roomListDTO;
    }

    @Override
    public RoomDTO getRoomById(Long roomId) {
        log.info("Fetching room with ID : {}",roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID "+ roomId));
        log.info("Fetched room with ID : {}",roomId);
        return modelMapper.map(room, RoomDTO.class);
    }

    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Checking room with ID : {}",roomId);
        boolean isRoomPresent = roomRepository.existsById(roomId);
        if(!isRoomPresent) throw new ResourceNotFoundException("Room not found with ID "+ roomId);
        log.info("Room with ID : {} is present in the system",roomId);

        log.info("Deleting room with ID : {}",roomId);
        roomRepository.deleteById(roomId);
        log.info("Deleted room with ID : {}",roomId);
        // TODO : delete all future inventories for this room

    }
}
