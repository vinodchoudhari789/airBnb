package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.HotelRepository;
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

    @Override
    public HotelDTO createNewHotel(HotelDTO hotelDTO) {
        log.info("Creating new hotel with name: {}",hotelDTO.getName());
        Hotel hotel = modelMapper.map(hotelDTO, Hotel.class);
        hotel.setActive(false);
        hotelRepository.save(hotel);
        log.info("Created a new hotel with ID : {}",hotelDTO.getId());
        return modelMapper.map(hotel,HotelDTO.class);
    }

    @Override
    public HotelDTO getHotelById(Long id) {
        log.info("Finding the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Fetched the hotel with ID : {}", id);
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
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
    public void deleteHotelById(Long id) {
        log.info("Checking the hotel with ID is present or not: {}", id);
        boolean exists =  hotelRepository.existsById(id);
        if(!exists) throw new ResourceNotFoundException("Hotel not found with ID : "+id);
        log.info("Hotel with ID : {} is present", id);

        hotelRepository.deleteById(id);
        log.info("Deleted the hotel with ID : {}", id);
        // do not hard delete it, soft delete it by
        // TODO : delete the inventories for this hotel
    }

    @Override
    public void activateHotelById(Long id) {
        log.info("Activating the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        hotel.setActive(true);
        hotelRepository.save(hotel);
        log.info("Activated the hotel with ID : {}", id);
        // TODO: Create Inventory for all the room for this hotel
    }
}
