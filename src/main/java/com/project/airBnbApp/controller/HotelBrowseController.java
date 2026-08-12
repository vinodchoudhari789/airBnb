package com.project.airBnbApp.controller;

import com.project.airBnbApp.dto.HotelDTO;
import com.project.airBnbApp.dto.HotelInfoDTO;
import com.project.airBnbApp.dto.HotelPriceDTO;
import com.project.airBnbApp.dto.HotelSearchRequestDTO;
import com.project.airBnbApp.service.HotelService;
import com.project.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@Slf4j
@RequiredArgsConstructor
public class HotelBrowseController {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDTO>> searchHotels(HotelSearchRequestDTO hotelSearchRequestDTO){

        Page<HotelPriceDTO> page = inventoryService.searchHotels(hotelSearchRequestDTO);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDTO> getHotelInfo(@PathVariable Long hotelId){
        HotelInfoDTO hotelInfo = hotelService.getHotelInfoById(hotelId);
        return ResponseEntity.ok(hotelInfo);
    }

}
