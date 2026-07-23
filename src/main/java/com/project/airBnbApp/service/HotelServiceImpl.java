package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.HotelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService{

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public HotelDTO createNewHotel(HotelDTO hotelDTO) {
        log.info("Creating new hotel with name: {}",hotelDTO.getName());
        Hotel hotel = modelMapper.map(hotelDTO, Hotel.class);
        hotel.setActive(false);
        hotelRepository.save(hotel);
        log.info("Created a new hotel with ID : {}",hotelDTO.getId());
        return modelMapper.map(hotel,HotelDTO.class);
    }

    @Override
    @Transactional
    public HotelDTO getHotelById(Long id) {
        log.info("Finding the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Fetched the hotel with ID : {}", id);
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    @Transactional
    public HotelDTO updateHotelById(Long id, HotelDTO hotelDTO) {
        log.info("Finding the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Fetched the hotel with ID : {}", id);
        modelMapper.map(hotelDTO, hotel);
        hotel.setId(id);

        // Arrays aren't affected by ModelMapper's collection-merge setting — handle manually
        if (hotelDTO.getAmenities() != null) {
            hotel.setAmenities(hotelDTO.getAmenities());
        }
        if (hotelDTO.getPhotos() != null) {
            hotel.setPhotos(hotelDTO.getPhotos());
        }
        hotel = hotelRepository.save(hotel);
        log.info("Updated the hotel with ID : {}", id);
        return  modelMapper.map(hotel,HotelDTO.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        log.info("Checking the hotel with ID is present or not: {}", id);
        Hotel hotel =  hotelRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Hotel with ID : {} is present", id);

        log.info("Deleting all future inventory for all rooms in hotel: {}", id);
        for(Room room: hotel.getRoomList()){
            inventoryService.deleteFutureInventories(room);
        }
        log.info("Successfully Deleted all future inventory for all rooms in hotel: {}", id);

        hotelRepository.deleteById(id);
        log.info("Deleted the hotel with ID : {}", id);
    }

    @Override
    @Transactional
    public void activateHotelById(Long id) {
        log.info("Activating the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        hotel.setActive(true);
        hotelRepository.save(hotel);
        log.info("Activated the hotel with ID : {}", id);

        // Assuming Only Do it Once
        log.info("Initializing one year of inventory for all rooms in hotel: {}", hotel.getId());
        for(Room room: hotel.getRoomList()){
            log.info("Initializing one year of inventory for room: {}", room.getId());
            inventoryService.initializeRoomForAYear(room);
            log.info("Successfully initialized one year of inventory for room: {}", room.getId());
        }
        log.info("Successfully initialized one year of inventory for all rooms in hotel: {}", hotel.getId());

    }
}
