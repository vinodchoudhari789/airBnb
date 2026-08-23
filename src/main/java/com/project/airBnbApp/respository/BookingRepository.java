package com.project.airBnbApp.respository;

import com.project.airBnbApp.entity.Booking;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);

    Page<Booking> findByHotel(Hotel hotel, Pageable pageable);

    Page<Booking> findByUser(User user, Pageable pageable);

    /**
     * Aggregates the hotel report stats (confirmed booking count, total
     * revenue, average revenue) in a single SQL query instead of loading
     * every booking row into memory and reducing in Java. Returns
     * [count, sum, avg] - sum/avg are null when there are no matching rows,
     * so the service layer coalesces those to BigDecimal.ZERO.
     */
    @Query("""
            SELECT COUNT(b), SUM(b.amount), AVG(b.amount)
            FROM Booking b
            WHERE b.hotel = :hotel
              AND b.createdAt BETWEEN :startDateTime AND :endDateTime
              AND b.bookingStatus = com.project.airBnbApp.entity.enums.BookingStatus.CONFIRMED
            """)
    Object[] getHotelReportStats(@Param("hotel") Hotel hotel,
                                  @Param("startDateTime") LocalDateTime startDateTime,
                                  @Param("endDateTime") LocalDateTime endDateTime);
}
