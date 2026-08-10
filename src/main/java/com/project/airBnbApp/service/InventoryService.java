package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelPriceDTO;
import com.project.airBnbApp.dto.HotelSearchRequestDTO;
import com.project.airBnbApp.dto.InventoryDTO;
import com.project.airBnbApp.dto.UpdateInventoryRequestDTO;
import com.project.airBnbApp.entity.Room;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelPriceDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequestDTO);

    List<InventoryDTO> getAllInventoryByRoom(Long roomId);

    void updateInventory(Long roomId, UpdateInventoryRequestDTO updateInventoryRequestDTO);
}
