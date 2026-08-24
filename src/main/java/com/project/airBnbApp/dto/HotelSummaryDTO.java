package com.project.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-facing hotel summary for search results. Deliberately holds only
 * the fields the search UI actually renders (see HotelCard.jsx on the
 * frontend) - never the Hotel entity itself, which carries an eager
 * `owner` (User) reference with no @JsonIgnore. That combination used to
 * leak the hotel owner's email and bcrypt password hash through the fully
 * unauthenticated GET /hotels/search endpoint.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelSummaryDTO {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
}
