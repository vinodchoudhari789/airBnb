package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.RoomDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnauthorizedException;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{

    private final ModelMapper modelMapper;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public RoomDTO createNewRoom(Long hotelId, RoomDTO roomDTO) {
        log.info("Starting creation of new room...");
        Room createdRoom = modelMapper.map(roomDTO, Room.class);

        log.info("Finding the hotel with ID : {}", hotelId);
        Hotel hotel =  hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        log.info("Fetched the hotel with ID : {}", hotelId);

        checkHotelBelongsToUser(hotel);

        createdRoom.setHotel(hotel);
        roomRepository.save(createdRoom);
        log.info("Finished creation of new room successfully!!!");

        log.info("Checking whether hotel is active or not : {}", hotel.getActive());
        if(hotel.getActive()){
            log.info("Initializing one year of inventory for room: {}", createdRoom.getId());
            inventoryService.initializeRoomForAYear(createdRoom);
            log.info("Successfully initialized one year of inventory for room: {}", createdRoom.getId());
        }

        return modelMapper.map(createdRoom, RoomDTO.class);
    }

    @Override
    @Transactional
    public List<RoomDTO> getAllRoomsInHotel(Long hotelId) {
        log.info("Fetching all rooms for hotel with ID : {}",hotelId);

        log.info("Finding the hotel with ID : {}", hotelId);
        Hotel hotel =  hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        log.info("Fetched the hotel with ID : {}", hotelId);

        checkHotelBelongsToUser(hotel);

        List<RoomDTO> roomListDTO = hotel.getRoomList()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDTO.class))
                .toList();
        log.info("Fetched all rooms for hotel with ID : {}",hotelId);
        return roomListDTO;
    }

    @Override
    @Transactional
    public RoomDTO getRoomById(Long roomId) {
        log.info("Fetching room with ID : {}",roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID "+ roomId));
        log.info("Fetched room with ID : {}",roomId);

        checkHotelBelongsToUser(room.getHotel());

        return modelMapper.map(room, RoomDTO.class);
    }

    @Override
    @Transactional
    public void deleteRoomById(Long roomId) {
        log.info("Checking room with ID : {}",roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(()->new ResourceNotFoundException("Room not found with ID "+ roomId));
        log.info("Room with ID : {} is present in the system",roomId);

        checkHotelBelongsToUser(room.getHotel());

        log.info("Deleting all inventory for room with ID : {}", roomId);
        inventoryService.deleteAllInventories(room);
        log.info("Successfully deleted all inventory for room with ID : {}", roomId);

        log.info("Deleting room with ID : {}",roomId);
        roomRepository.deleteById(roomId);
        log.info("Deleted room with ID : {}",roomId);
    }

    @Override
    @Transactional
    public RoomDTO updateRoomById(Long hotelId, Long roomId, RoomDTO roomDTO) {

        log.info("Checking room with ID : {}",roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(()->new ResourceNotFoundException("Room not found with ID "+ roomId));
        log.info("Room with ID : {} is present in the system",roomId);

        checkHotelBelongsToUser(room.getHotel());

        modelMapper.map(roomDTO, room);
        room.setId(roomId);

        // TODO: if price or inventory is updated then update the inventory for this room.

        log.info("Updated room with ID : {}",roomId);
        return modelMapper.map(room, RoomDTO.class);
    }

    private void checkHotelBelongsToUser(Hotel hotel){
        log.info("Checking if hotel belongs to the user or not");
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())){
            throw new UnauthorizedException("This user does not own this hotel with id : "+hotel.getId());
        }
        log.info("Confirmed, This hotel belongs to the user.");
    }
}
