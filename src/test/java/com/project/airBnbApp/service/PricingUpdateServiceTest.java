package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.HotelMinPrice;
import com.project.airBnbApp.entity.Inventory;
import com.project.airBnbApp.respository.HotelMinPriceRepository;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.InventoryRepository;
import com.project.airBnbApp.strategy.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Verifies the fix for the PricingUpdateService N+1 query problem.
 *
 * Previously, updateHotelMinPrice() called
 * hotelMinPriceRepository.findByHotelAndDate(hotel, date) once per date
 * (~365 queries/hotel/year). It now fetches the whole date range in a
 * single findByHotelAndDateBetween() call and resolves each date from an
 * in-memory map.
 */
@ExtendWith(MockitoExtension.class)
class PricingUpdateServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private HotelMinPriceRepository hotelMinPriceRepository;

    @Mock
    private PricingService pricingService;

    private PricingUpdateService pricingUpdateService;

    private Hotel hotel;

    @BeforeEach
    void setUp() {
        pricingUpdateService = new PricingUpdateService(
                hotelRepository, inventoryRepository, hotelMinPriceRepository, pricingService);

        hotel = new Hotel();
        hotel.setId(1L);
    }

    private Inventory inventoryOn(LocalDate date, BigDecimal price) {
        return Inventory.builder()
                .hotel(hotel)
                .date(date)
                .price(price)
                .bookedCount(0)
                .reservedCount(0)
                .totalCount(10)
                .surgeFactor(BigDecimal.ONE)
                .city("Bangalore")
                .closed(false)
                .build();
    }

    @Test
    void updateHotelPrices_fetchesExistingMinPricesInOneRangeQuery_notPerDate() {
        LocalDate day1 = LocalDate.now();
        LocalDate day2 = day1.plusDays(1);
        LocalDate day3 = day1.plusDays(2);

        // Two rooms priced for the same 3 days -> min price per day should be picked
        List<Inventory> inventoryList = List.of(
                inventoryOn(day1, new BigDecimal("1500")),
                inventoryOn(day1, new BigDecimal("1200")),
                inventoryOn(day2, new BigDecimal("1800")),
                inventoryOn(day3, new BigDecimal("2000"))
        );

        when(inventoryRepository.findByHotelAndDateBetween(eq(hotel), any(), any()))
                .thenReturn(inventoryList);
        when(pricingService.calculateDynamicPricing(any(Inventory.class)))
                .thenAnswer(invocation -> ((Inventory) invocation.getArgument(0)).getPrice());

        // Only day1 already has a cached row; day2 and day3 are new
        HotelMinPrice existingDay1 = new HotelMinPrice(hotel, day1);
        existingDay1.setPrice(new BigDecimal("1600")); // stale value, should be overwritten
        when(hotelMinPriceRepository.findByHotelAndDateBetween(eq(hotel), any(), any()))
                .thenReturn(List.of(existingDay1));

        pricingUpdateService.updateHotelPrices(hotel);

        // The old per-date lookup must never be used
        verify(hotelMinPriceRepository, never()).findByHotelAndDate(any(), any());

        // The range query is called exactly once for the whole hotel
        verify(hotelMinPriceRepository, times(1))
                .findByHotelAndDateBetween(eq(hotel), any(), any());

        // saveAll is called exactly once (bulk save), not once per date
        ArgumentCaptor<List<HotelMinPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(hotelMinPriceRepository, times(1)).saveAll(captor.capture());

        List<HotelMinPrice> saved = captor.getValue();
        assertThat(saved).hasSize(3);

        HotelMinPrice savedDay1 = saved.stream().filter(p -> p.getDate().equals(day1)).findFirst().orElseThrow();
        HotelMinPrice savedDay2 = saved.stream().filter(p -> p.getDate().equals(day2)).findFirst().orElseThrow();
        HotelMinPrice savedDay3 = saved.stream().filter(p -> p.getDate().equals(day3)).findFirst().orElseThrow();

        // Existing row for day1 is reused (same object) and its price is refreshed to the new min
        assertThat(savedDay1).isSameAs(existingDay1);
        assertThat(savedDay1.getPrice()).isEqualByComparingTo("1200");

        // New rows are created for dates with no cached price
        assertThat(savedDay2.getPrice()).isEqualByComparingTo("1800");
        assertThat(savedDay3.getPrice()).isEqualByComparingTo("2000");
    }

    @Test
    void updateHotelPrices_skipsMinPriceLookup_whenHotelHasNoInventory() {
        when(inventoryRepository.findByHotelAndDateBetween(eq(hotel), any(), any()))
                .thenReturn(List.of());

        pricingUpdateService.updateHotelPrices(hotel);

        verify(inventoryRepository, times(1)).saveAll(anyList());
        verifyNoInteractions(hotelMinPriceRepository);
    }
}
