package com.project.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomPriceDTO {

    private Long roomId;

    private Integer nights;

    private Integer roomsCount;

    private BigDecimal totalPrice;

    // false if the room isn't actually available for the full date range at
    // this roomsCount - totalPrice will be understated (missing days) if so,
    // the frontend should treat that as "unavailable" rather than a real price.
    private boolean available;
}
