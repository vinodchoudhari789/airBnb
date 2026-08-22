package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelPriceDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.HotelMinPrice;
import com.project.airBnbApp.entity.Inventory;
import com.project.airBnbApp.respository.HotelMinPriceRepository;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.InventoryRepository;
import com.project.airBnbApp.strategy.PricingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PricingUpdateService {

    // Scheduler

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final PricingService pricingService;

//     @Scheduled(cron = "*/30 * * * * *")  // for every 5 seconds
//    @Scheduled(cron = "0 * * * * *")  // for every minute
    @Scheduled(cron = "0 0 * * * *")
    public void updatePrice(){
        log.info("Running scheduler for updating prices of hotel");

        int page = 0;
        int batchSize = 100;

        while(true){
            Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batchSize));
            if(hotelPage.isEmpty()){
                break;
            }
            hotelPage.getContent().forEach(hotel -> updateHotelPrices(hotel));

            page ++;
        }
    }

    /**
     * Recalculates Inventory.price for every future inventory row of this
     * hotel, and refreshes the hotel_min_price cache used by guest search.
     * Called by the hourly scheduler (updatePrice) for all hotels, and also
     * called immediately after a room is edited (see RoomServiceImpl) so a
     * manager's price correction doesn't sit stale for up to an hour.
     */
    public void updateHotelPrices(Hotel hotel){
        log.info("Updating hotel prices for hotel ID: {}", hotel.getId());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);

        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel,startDate,endDate);

        updateInventoryPrices(inventoryList);

        updateHotelMinPrice(hotel, inventoryList);
    }

    private void updateInventoryPrices( List<Inventory> inventoryList){
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        inventoryRepository.saveAll(inventoryList);
    }

    private void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList) {
        // Compute minimum price per day fot the hotel
        Map<LocalDate, BigDecimal> dailyMinPrices = inventoryList.stream()
                .collect(Collectors.groupingBy(
                    Inventory::getDate,
                    Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(BigDecimal.ZERO)));

        if (dailyMinPrices.isEmpty()) {
            return;
        }

        // Fetch every existing HotelMinPrice row for this hotel's date range in ONE query,
        // instead of calling findByHotelAndDate() per-date (previously ~365 queries/hotel).
        LocalDate rangeStart = Collections.min(dailyMinPrices.keySet());
        LocalDate rangeEnd = Collections.max(dailyMinPrices.keySet());

        Map<LocalDate, HotelMinPrice> existingByDate = hotelMinPriceRepository
                .findByHotelAndDateBetween(hotel, rangeStart, rangeEnd)
                .stream()
                .collect(Collectors.toMap(HotelMinPrice::getDate, Function.identity()));

        // Prepare HotelPrice entities in bulk, reusing existing rows where present
        // and creating new ones only for dates that don't have a cached price yet.
        List<HotelMinPrice> hotelPriceList = new ArrayList<>();
        dailyMinPrices.forEach((date, price) -> {
            HotelMinPrice hotelMinPriceObj = existingByDate.getOrDefault(date, new HotelMinPrice(hotel, date));
            hotelMinPriceObj.setPrice(price);
            hotelPriceList.add(hotelMinPriceObj);
        });

        // save all hotelPrice entities in bulk (single batched save instead of N calls)
        hotelMinPriceRepository.saveAll(hotelPriceList);

    }


}
