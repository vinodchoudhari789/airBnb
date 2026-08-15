package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.dto.HotelInfoDTO;

import java.util.List;

public interface HotelService {
    HotelDTO createNewHotel(HotelDTO hotelDTO);

    HotelDTO getHotelById(Long id);

    HotelDTO updateHotelById(Long id,HotelDTO hotelDTO);

    void deleteHotelById(Long id);

    void activateHotelById(Long id);

    void deactivateHotelById(Long id);

    HotelInfoDTO getHotelInfoById(Long hotelId);

    List<HotelDTO> getAllHotels();
}
