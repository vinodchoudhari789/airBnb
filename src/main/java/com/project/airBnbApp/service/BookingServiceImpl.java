package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDTO;
import com.project.airBnbApp.dto.BookingRequestDTO;
import com.project.airBnbApp.dto.GuestDTO;
import com.project.airBnbApp.dto.HotelReportDTO;
import com.project.airBnbApp.entity.*;
import com.project.airBnbApp.entity.enums.BookingStatus;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnauthorizedException;
import com.project.airBnbApp.respository.*;
import com.project.airBnbApp.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.project.airBnbApp.util.AppUtils.getCurrentUser;

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
    private final PricingService pricingService;

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
        inventoryRepository.initBooking(bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPriceForOneRoom(inventoryList);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        log.info("Creating new booking");
        Booking newBooking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .bookingStatus(BookingStatus.RESERVED)
                .build();

        newBooking = bookingRepository.save(newBooking);

        log.info("Initialized Booking");
        return modelMapper.map(newBooking, BookingDTO.class);
    }

    @Override
    @Transactional
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
    @Transactional
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

    @Override
    @Transactional
    public void capturePayment(Event event) {
        log.info("capturePayment start");
        if("checkout.session.completed".equals(event.getType())){

            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

            if(session == null) return;
            String sessionId = session.getId();
            Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
                    .orElseThrow(()-> new ResourceNotFoundException("Booking not found for Session Id : "+sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);

            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            log.info("Locked reserved inventory");

            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            log.info("Successfully confirmed the booking for Id : {}", booking.getId());

        }else{
            log.warn("Unhandled event type : {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        log.info("Starting the process of cancellation");

        log.info("Checking if booking exists or not");
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id : "+ bookingId));

        User user = getCurrentUser();
        log.info("Checking if booking is of current user or not");
        if(!user.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id : "+user.getId());
        }

        if(booking.getBookingStatus() != BookingStatus.CONFIRMED){
            throw new IllegalStateException("Only confirmed booking can be cancelled.");
        }

        log.info("Updating Booking Status to CANCELLED");
        booking.setBookingStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
        log.info("Saved booking with updated status");

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        log.info("Locked reserved inventory");

        inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        log.info("Updated reserved count from inventory");

        log.info("Successfully cancelled the booking for Id : {}", booking.getId());


        //handle the refund
        try{
            log.info("Creating refund session");
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();

            Refund.create(refundParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<BookingDTO> getAllBookingsInHotelById(Long hotelId) {
        log.info("Fetching hotel with Id : {}",hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        log.info("Fetched hotel with Id : {}",hotelId);

        User user = getCurrentUser();
        log.info("Checking if hotel is of current user or not");
        if(!user.equals(hotel.getOwner())){
            throw new UnauthorizedException("User does not own this hotel with id : "+hotelId);
        }


        List<Booking> bookings = bookingRepository.findByHotel(hotel);
        log.info("Fetched all bookings of hotel with Id : {}",hotelId);

        return bookings.stream()
                .map((element) -> modelMapper.map(element, BookingDTO.class))
                .toList();
    }

    @Override
    public HotelReportDTO getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching hotel with Id : {}",hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        log.info("Fetched hotel with Id : {}",hotelId);

        User user = getCurrentUser();
        log.info("Checking if hotel is of current user or not");
        if(!user.equals(hotel.getOwner())){
            throw new UnauthorizedException("User does not own this hotel with id : "+hotelId);
        }

        log.info("Generating report for hotel with Id : {}", hotelId);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);
        log.info("Fetched all confirmed bookings for hotel with Id : {}", hotelId);


        Long totalConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenueOfConfirmedBookings = totalConfirmedBookings == 0 ? BigDecimal.ZERO :
                totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP);

        return new HotelReportDTO(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenueOfConfirmedBookings);
    }

    @Override
    public List<BookingDTO> getMyBookings() {
        User user = getCurrentUser();
        log.info("Fetching all bookings of user : {}", user.getName());

        List<Booking> bookings = bookingRepository.findByUser(user);
        log.info("Fetched all bookings of user : {}", user.getName());

        return bookings.stream()
                .map((element) -> modelMapper.map(element, BookingDTO.class))
                .collect(Collectors.toList());
    }

    private boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
}
