package com.project.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat row shape used only as the target of the
 * HotelMinPriceRepository.findHotelWithAvailableInventory JPQL constructor
 * expression - JPQL constructor expressions can't nest another `new` call
 * as an argument, so this can't directly produce a HotelPriceDTO(HotelSummaryDTO, price).
 * InventoryServiceImpl.searchHotels remaps each row into that nested shape
 * before returning it to the controller.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelSearchRowDTO {
    private Long hotelId;
    private String hotelName;
    private String hotelCity;
    private String[] photos;
    private String[] amenities;
    private Double price;
}
