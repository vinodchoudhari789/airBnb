package com.project.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public search-result row. `hotel` is intentionally HotelSummaryDTO, not
 * the Hotel entity - see HotelSummaryDTO's javadoc for why. Field name/JSON
 * shape ({ hotel: {...}, price }) is unchanged from before, so no frontend
 * changes are needed - only what's inside `hotel` got smaller and safer.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceDTO {

    private HotelSummaryDTO hotel;
    private Double price;
}
