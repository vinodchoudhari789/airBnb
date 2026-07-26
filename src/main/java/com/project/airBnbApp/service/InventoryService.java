package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.dto.HotelPriceDTO;
import com.project.airBnbApp.dto.HotelSearchRequestDTO;
import com.project.airBnbApp.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelPriceDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequestDTO);
}
