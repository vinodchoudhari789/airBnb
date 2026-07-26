package com.project.airBnbApp.respository;

import com.project.airBnbApp.dto.HotelPriceDTO;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {

    @Query("""
        SELECT com.project.airBnbApp.dto.HotelPriceDTO(i.hotel, AVG(i.price))
        FROM HotelMinPrice i
        WHERE i.hotel.city = :city
            AND i.date BETWEEN :startDate AND :endDate
            AND i.hotel.active = true
        GROUP BY i.hotel
        """)
    Page<HotelPriceDTO> findHotelWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}