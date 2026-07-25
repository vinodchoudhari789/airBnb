package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;
import com.project.airBnbApp.entity.*;
import com.project.airBnbApp.entity.enums.BookingStatus;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.BookingRepository;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.InventoryRepository;
import com.project.airBnbApp.respository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{
    private final InventoryRepository inventoryRepository;

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public BookingDTO initializeBooking(BookingRequestDTO bookingRequest) {
        log.info("Initializing Booking for hotel : {}, room : {}, date {}-{}",
                bookingRequest.getHotelId(),bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate());

        log.info("Fetching hotel with Id : {}",bookingRequest.getHotelId());
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : "+bookingRequest.getHotelId()));
        log.info("Fetched hotel with Id : {}",bookingRequest.getHotelId());

        log.info("Fetching room with Id : {}",bookingRequest.getRoomId());
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID : "+bookingRequest.getRoomId()));
        log.info("Fetched room with Id : {}",bookingRequest.getRoomId());

        log.info("Fetching List of Inventory with room Id : {} , between date {} - {}",
                bookingRequest.getRoomId(), bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate());
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        log.info("Fetched List of Inventory with room Id : {}, between date {} - {}",
                bookingRequest.getRoomId(), bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate());

        // Checking if the fetch inventory count match the required book count
        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate()) + 1;

        if(inventoryList.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }

        // Reserve the room/update the booked count of inventories
        for(Inventory inventory: inventoryList){
            inventory.setBookedCount(inventory.getBookedCount() + bookingRequest.getRoomsCount());
        }

        log.info("Updated inventories list");
        inventoryRepository.saveAll(inventoryList);

        User user = new User();
        user.setId(1L);
        // TODO : Remove Dummy User

        // TODO : Calculate Dynamic Amount

        log.info("Creating new booking");
        Booking newBooking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(user)
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .bookingStatus(BookingStatus.RESERVED)
                .build();

        newBooking = bookingRepository.save(newBooking);

        log.info("Initialized Booking");
        return modelMapper.map(newBooking, BookingDTO.class);
    }
}
