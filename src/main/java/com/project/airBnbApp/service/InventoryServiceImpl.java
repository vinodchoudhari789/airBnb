package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.dto.HotelSearchRequestDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Inventory;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.respository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService{

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        log.info("Starting with initialization from {} of room with ID : {}",today,room.getId());
        LocalDate endDate = today.plusYears(1);
        for(; !today.isAfter(endDate);  today=today.plusDays(1)){
            Inventory inventory = Inventory
                    .builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }
        log.info("Finished with initialization till {} of room with ID : {}",today,room.getId());
    }

    @Override
    public void deleteAllInventories(Room room) {
        log.info("Starting with deletion of inventories of room with ID : {}",room.getId());

        inventoryRepository.deleteByRoom(room);

        log.info("Finished with deletion of inventories of room with ID : {}",room.getId());

    }

    @Override
    public Page<HotelDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequestDTO) {
        log.info("Searching hotels for {} city, from {} to {}",
                hotelSearchRequestDTO.getCity(), hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate());

        Pageable pageable = PageRequest.of(hotelSearchRequestDTO.getPage(), hotelSearchRequestDTO.getSize());
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequestDTO.getStartDate(),hotelSearchRequestDTO.getEndDate()) + 1;

        Page<Hotel> hotelPage = inventoryRepository.findHotelWithAvailableInventory(hotelSearchRequestDTO.getCity(),
                hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate(),
                hotelSearchRequestDTO.getRoomsCount(), dateCount, pageable);

        log.info("Completed search hotels for {} city, from {} to {}",
                hotelSearchRequestDTO.getCity(), hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate());
        return hotelPage.map((element) -> modelMapper.map(element, HotelDTO.class));


    }
}
