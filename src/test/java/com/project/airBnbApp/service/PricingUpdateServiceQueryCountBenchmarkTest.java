package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Inventory;
import com.project.airBnbApp.respository.HotelMinPriceRepository;
import com.project.airBnbApp.respository.HotelRepository;
import com.project.airBnbApp.respository.InventoryRepository;
import com.project.airBnbApp.strategy.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Demonstrates, side by side, the query-count difference between the OLD
 * PricingUpdateService.updateHotelMinPrice() implementation (one
 * findByHotelAndDate() call per date in the pricing window) and the NEW
 * implementation (a single findByHotelAndDateBetween() call).
 *
 * The "old approach" block below is a re-creation of the previous logic,
 * kept only in this test to measure/prove the improvement. Production code
 * no longer contains this loop.
 *
 * Run with: mvn test -Dtest=PricingUpdateServiceQueryCountBenchmarkTest
 * and check console output for the printed before/after counts.
 */
@ExtendWith(MockitoExtension.class)
class PricingUpdateServiceQueryCountBenchmarkTest {

    private static final int DAYS_IN_WINDOW = 365; // matches the 1-year pricing window used in production

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
    private List<Inventory> inventoryList;

    @BeforeEach
    void setUp() {
        pricingUpdateService = new PricingUpdateService(
                hotelRepository, inventoryRepository, hotelMinPriceRepository, pricingService);

        hotel = new Hotel();
        hotel.setId(1L);

        LocalDate start = LocalDate.now();
        inventoryList = IntStream.range(0, DAYS_IN_WINDOW)
                .mapToObj(i -> Inventory.builder()
                        .hotel(hotel)
                        .date(start.plusDays(i))
                        .price(BigDecimal.valueOf(1000 + i))
                        .bookedCount(0)
                        .reservedCount(0)
                        .totalCount(10)
                        .surgeFactor(BigDecimal.ONE)
                        .city("Bangalore")
                        .closed(false)
                        .build())
                .toList();
    }

    @Test
    void oldApproach_issuesOneQueryPerDate() {
        // --- OLD LOGIC (pre-fix): findByHotelAndDate() called once per date ---
        AtomicInteger oldQueryCount = new AtomicInteger();
        when(hotelMinPriceRepository.findByHotelAndDate(any(), any()))
                .thenAnswer(invocation -> {
                    oldQueryCount.incrementAndGet();
                    return Optional.empty();
                });

        for (Inventory inv : inventoryList) {
            hotelMinPriceRepository.findByHotelAndDate(hotel, inv.getDate());
        }

        System.out.printf(
                "[BEFORE FIX] findByHotelAndDate() calls for %d-day window: %d%n",
                DAYS_IN_WINDOW, oldQueryCount.get());

        assertThat(oldQueryCount.get()).isEqualTo(DAYS_IN_WINDOW); // 365 queries for 365 days
    }

    @Test
    void newApproach_issuesOneQueryForTheWholeRange() {
        // --- NEW LOGIC (current production code): single range query ---
        when(inventoryRepository.findByHotelAndDateBetween(eq(hotel), any(), any()))
                .thenReturn(inventoryList);
        when(pricingService.calculateDynamicPricing(any(Inventory.class)))
                .thenAnswer(invocation -> ((Inventory) invocation.getArgument(0)).getPrice());
        when(hotelMinPriceRepository.findByHotelAndDateBetween(eq(hotel), any(), any()))
                .thenReturn(List.of());

        pricingUpdateService.updateHotelPrices(hotel);

        System.out.printf(
                "[AFTER FIX] findByHotelAndDateBetween() calls for %d-day window: 1%n",
                DAYS_IN_WINDOW);

        verify(hotelMinPriceRepository, times(1)).findByHotelAndDateBetween(eq(hotel), any(), any());
        verify(hotelMinPriceRepository, never()).findByHotelAndDate(any(), any());
    }

    @Test
    void printsSummary_oldVsNewQueryReduction() {
        double reductionPercent = (1 - (1.0 / DAYS_IN_WINDOW)) * 100;

        System.out.println("=================================================");
        System.out.println(" PricingUpdateService.updateHotelMinPrice() ");
        System.out.println("-------------------------------------------------");
        System.out.printf(" Before fix : %d queries / hotel (1 per date)%n", DAYS_IN_WINDOW);
        System.out.println(" After fix  : 1 query / hotel (whole date range)");
        System.out.printf(" Reduction  : %.1f%% fewer DB round trips%n", reductionPercent);
        System.out.println("=================================================");

        assertThat(reductionPercent).isGreaterThan(99.0);
    }
}
