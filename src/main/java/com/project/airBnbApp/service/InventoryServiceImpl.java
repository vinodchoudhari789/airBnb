package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelPriceDTO;
import com.project.airBnbApp.dto.HotelSearchRequestDTO;
import com.project.airBnbApp.dto.InventoryDTO;
import com.project.airBnbApp.dto.UpdateInventoryRequestDTO;
import com.project.airBnbApp.entity.Inventory;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.HotelMinPriceRepository;
import com.project.airBnbApp.respository.InventoryRepository;
import com.project.airBnbApp.respository.RoomRepository;
import jakarta.transaction.Transactional;
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
import java.util.List;

import static com.project.airBnbApp.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService{
    private final RoomRepository roomRepository;

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final HotelMinPriceRepository hotelMinPriceRepository;

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
                    .reservedCount(0)
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
    public Page<HotelPriceDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequestDTO) {
        log.info("Searching hotels for {} city, from {} to {}",
                hotelSearchRequestDTO.getCity(), hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate());

        Pageable pageable = PageRequest.of(hotelSearchRequestDTO.getPage(), hotelSearchRequestDTO.getSize());
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequestDTO.getStartDate(),hotelSearchRequestDTO.getEndDate()) + 1;

        // business logic  -- if 90 Days < then hotel min price and if 90 Days

        Page<HotelPriceDTO> hotelPage = hotelMinPriceRepository.findHotelWithAvailableInventory(hotelSearchRequestDTO.getCity(),
                hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate(), pageable);

        log.info("Completed search hotels for {} city, from {} to {}",
                hotelSearchRequestDTO.getCity(), hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate());
        return hotelPage;
    }

//    @Override
//    public Page<HotelDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequestDTO) {
//        log.info("Searching hotels for {} city, from {} to {}",
//                hotelSearchRequestDTO.getCity(), hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate());
//
//        Pageable pageable = PageRequest.of(hotelSearchRequestDTO.getPage(), hotelSearchRequestDTO.getSize());
//        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequestDTO.getStartDate(),hotelSearchRequestDTO.getEndDate()) + 1;
//
//        Page<Hotel> hotelPage = inventoryRepository.findHotelWithAvailableInventory(hotelSearchRequestDTO.getCity(),
//                hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate(),
//                hotelSearchRequestDTO.getRoomsCount(), dateCount, pageable);
//
//        log.info("Completed search hotels for {} city, from {} to {}",
//                hotelSearchRequestDTO.getCity(), hotelSearchRequestDTO.getStartDate(), hotelSearchRequestDTO.getEndDate());
//        return hotelPage.map((element) -> modelMapper.map(element, HotelDTO.class));
//    }

    @Override
    @Transactional
    public List<InventoryDTO> getAllInventoryByRoom(Long roomId) {
        log.info("Fetching all inventory by room for room with Id : {}",roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with id : "+roomId));

        User user = getCurrentUser();
        if(!user.equals(room.getHotel().getOwner())) throw new ResourceNotFoundException("You are not the owner of room with roomId : "+roomId);

        return inventoryRepository.findByRoomOrderByDate(room)
                .stream()
                .map((element) -> modelMapper.map(element, InventoryDTO.class))
                .toList();
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDTO updateInventoryRequestDTO) {
        log.info("Updating all inventory by room for room with Id : {} between date range : {} - {}",roomId,
                updateInventoryRequestDTO.getStartDate(),updateInventoryRequestDTO.getEndDate());

        inventoryRepository.getInventoryAndLockBeforeUpdate(roomId,
                updateInventoryRequestDTO.getStartDate(), updateInventoryRequestDTO.getEndDate());
        log.info("Locked Inventory Before Update");

        inventoryRepository.updateInventory(roomId,
                updateInventoryRequestDTO.getStartDate(), updateInventoryRequestDTO.getEndDate(),
                updateInventoryRequestDTO.getClosed(), updateInventoryRequestDTO.getSurgeFactor() );

        log.info("Updated all inventory by room for room with Id : {} between date range : {} - {}",roomId,
                updateInventoryRequestDTO.getStartDate(),updateInventoryRequestDTO.getEndDate());

    }
}
