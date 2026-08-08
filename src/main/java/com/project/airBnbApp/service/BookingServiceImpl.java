package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;
import com.project.airBnbApp.dto.GuestDTO;
import com.project.airBnbApp.entity.*;
import com.project.airBnbApp.entity.enums.BookingStatus;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnauthorizedException;
import com.project.airBnbApp.respository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{
    private final GuestRepository guestRepository;
    private final InventoryRepository inventoryRepository;

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final CheckoutService checkoutService;

    @Value("${frontend.url}")
    private String frontendUrl;

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
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }

        log.info("Updated inventories list");
        inventoryRepository.saveAll(inventoryList);

        // TODO : Calculate Dynamic Amount

        log.info("Creating new booking");
        Booking newBooking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .bookingStatus(BookingStatus.RESERVED)
                .build();

        newBooking = bookingRepository.save(newBooking);

        log.info("Initialized Booking");
        return modelMapper.map(newBooking, BookingDTO.class);
    }

    @Override
    public BookingDTO addGuests(Long bookingId, List<GuestDTO> guestsDTOList) {
        log.info("Adding Guests for Booking : {}", bookingId );

        log.info("Fetching booking with Id : {}",bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new ResourceNotFoundException("Booking not found with ID : "+bookingId));
        log.info("Fetched booking with Id : {}",bookingId);

        User user = getCurrentUser();

        log.info("Checking if booking is of current user or not");
        if(!user.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id : "+user.getId());
        }


        log.info("Checking if booking is expired or not");
        if(hasBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired");
        }

        log.info("Checking if booking is reserved or not");
        if(booking.getBookingStatus() != BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under RESERVED status, cannot add guests");
        }

        log.info("Adding guests in booking");
        for(GuestDTO guestDTO: guestsDTOList){
            Guest guest = modelMapper.map(guestDTO, Guest.class);
            guest.setUser(getCurrentUser());
            guest = guestRepository.save(guest);
            booking.getGuestSet().add(guest);
        }

        log.info("Updating Booking Status to GUESTS_ADDED");
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);

        log.info("Guests Added Successfully!!!");
        return modelMapper.map(booking, BookingDTO.class);
    }

    @Override
    public String initiatePayment(Long bookingId) {
        log.info("Checking if booking exists or not");
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id : "+ bookingId));

        User user = getCurrentUser();

        log.info("Checking if booking is of current user or not");
        if(!user.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id : "+user.getId());
        }


        log.info("Checking if booking is expired or not");
        if(hasBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired");
        }

         String sessionUrl = checkoutService.getCheckoutSession(booking,
                frontendUrl+"/payments/success",
                frontendUrl+"/payments/failure" );

        log.info("Updating Booking Status to PAYMENT_PENDING");
        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);

        log.info("Saving Booking");
        bookingRepository.save(booking);

        return sessionUrl;
    }

    private boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser(){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user;
    }
}
