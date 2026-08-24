package com.project.airBnbApp.respository;

import com.project.airBnbApp.dto.HotelSearchRowDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {

    // Projects only the fields the public search UI needs (see
    // HotelSearchRowDTO/HotelSummaryDTO javadocs) instead of the whole Hotel
    // entity - the entity carries an eager, unguarded `owner` (User)
    // reference that used to leak into this fully unauthenticated endpoint.
    // AVG(i.price) intentionally left as-is here (a separate, already-
    // discussed correctness question, not part of this DTO/security fix).
    @Query("""
        SELECT new com.project.airBnbApp.dto.HotelSearchRowDTO(
            i.hotel.id, i.hotel.name, i.hotel.city, i.hotel.photos, i.hotel.amenities, AVG(i.price))
        FROM HotelMinPrice i
        WHERE i.hotel.city = :city
            AND i.date BETWEEN :startDate AND :endDate
            AND i.hotel.active = true
        GROUP BY i.hotel
        """)
    Page<HotelSearchRowDTO> findHotelWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);

    /**
     * Fetches all existing HotelMinPrice rows for a hotel across a date range
     * in a single query, so callers can build an in-memory lookup instead of
     * querying per-date (avoids N+1 queries when refreshing a hotel's price cache).
     */
    List<HotelMinPrice> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);
}