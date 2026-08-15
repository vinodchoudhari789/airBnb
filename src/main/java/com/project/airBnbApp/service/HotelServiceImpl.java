package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.dto.HotelInfoDTO;
import com.project.airBnbApp.dto.RoomDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnauthorizedException;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.InventoryRepository;
import com.project.airBnbApp.respository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.project.airBnbApp.util.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService{

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public HotelDTO createNewHotel(HotelDTO hotelDTO) {
        log.info("Creating new hotel with name: {}",hotelDTO.getName());
        Hotel hotel = modelMapper.map(hotelDTO, Hotel.class);
        hotel.setActive(false);

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel.setOwner(user);

        hotelRepository.save(hotel);
        log.info("Created a new hotel with ID : {}",hotelDTO.getId());
        return modelMapper.map(hotel,HotelDTO.class);
    }

    @Override
    @Transactional
    public HotelDTO getHotelById(Long id) {
        log.info("Finding the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Fetched the hotel with ID : {}", id);

        checkHotelBelongsToUser(hotel);

        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    @Transactional
    public HotelDTO updateHotelById(Long id, HotelDTO hotelDTO) {
        log.info("Finding the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Fetched the hotel with ID : {}", id);

        checkHotelBelongsToUser(hotel);

        // Preserve active status regardless of what's in hotelDTO - the
        // frontend's edit form doesn't send `active` (it's not meant to be
        // editable there), which deserializes as null and would otherwise
        // get blanket-mapped over the existing value below, silently
        // deactivating the hotel. Activation is exclusively the job of
        // activateHotelById.
        Boolean wasActive = hotel.getActive();

        modelMapper.map(hotelDTO, hotel);
        hotel.setId(id);
        hotel.setActive(wasActive);

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
    @Transactional
    public void deleteHotelById(Long id) {
        log.info("Checking the hotel with ID is present or not: {}", id);
        Hotel hotel =  hotelRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Hotel not found with ID : "+id));
        log.info("Hotel with ID : {} is present", id);

        checkHotelBelongsToUser(hotel);

        log.info("Deleting all inventory for all rooms in hotel: {}", id);
        for(Room room: hotel.getRoomList()){
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }
        log.info("Successfully deleted future inventory for all rooms in hotel: {}", id);

        hotelRepository.deleteById(id);
        log.info("Deleted the hotel with ID : {}", id);
    }

    @Override
    @Transactional
    public void activateHotelById(Long id) {
        log.info("Activating the hotel with ID : {}", id);
        Hotel hotel =  hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));

        checkHotelBelongsToUser(hotel);

        hotel.setActive(true);
        hotelRepository.save(hotel);
        log.info("Activated the hotel with ID : {}", id);

        // Idempotent by design: only initialize a room's inventory if it
        // doesn't already have any. This makes activation safe to call any
        // number of times (e.g. after a deactivate/reactivate cycle) without
        // duplicating inventory rows, and also correctly backfills inventory
        // for any room that was added while the hotel was inactive (those
        // don't get auto-initialized at creation time).
        log.info("Ensuring one year of inventory exists for all rooms in hotel: {}", hotel.getId());
        for(Room room: hotel.getRoomList()){
            if (inventoryRepository.existsByRoom(room)) {
                log.info("Room {} already has inventory, skipping", room.getId());
                continue;
            }
            log.info("Initializing one year of inventory for room: {}", room.getId());
            inventoryService.initializeRoomForAYear(room);
            log.info("Successfully initialized one year of inventory for room: {}", room.getId());
        }
        log.info("Successfully ensured inventory for all rooms in hotel: {}", hotel.getId());

    }

    @Override
    @Transactional
    public void deactivateHotelById(Long id) {
        log.info("Deactivating the hotel with ID : {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID : "+id));

        checkHotelBelongsToUser(hotel);

        // Just a visibility flag - no inventory side effects, so this is
        // trivially safe to call any number of times. Existing inventory
        // and bookings are left intact; the hotel simply stops appearing
        // in guest search until reactivated.
        hotel.setActive(false);
        hotelRepository.save(hotel);
        log.info("Deactivated the hotel with ID : {}", id);
    }


    // Public Method
    @Override
    public HotelInfoDTO getHotelInfoById(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID : "+hotelId));
        List<RoomDTO> rooms = hotel.getRoomList()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDTO.class))
                .toList();
        return new HotelInfoDTO(modelMapper.map(hotel, HotelDTO.class), rooms);
    }

    @Override
    public List<HotelDTO> getAllHotels() {
        User user  = getCurrentUser();
        log.info("Getting all hotels for the admin user with Id : {}",user.getId());
        List<Hotel> hotelList = hotelRepository.findByOwner(user);
        return hotelList.stream()
                .map((element) -> modelMapper.map(element, HotelDTO.class))
                .toList();
    }

    private void checkHotelBelongsToUser(Hotel hotel){
        log.info("Checking if hotel belongs to the user or not");
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())){
            throw new UnauthorizedException("This user does not own this hotel with id : "+hotel.getId());
        }
        log.info("Confirmed, This hotel belongs to the user.");
    }
}
