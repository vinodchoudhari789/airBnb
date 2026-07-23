package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.Inventory;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.respository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService{

    private final InventoryRepository inventoryRepository;

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
}
