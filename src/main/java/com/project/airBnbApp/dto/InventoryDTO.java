package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventoryDTO {

    private Long id;

    private Hotel hotel;

    private Room room;

    private Date date;

    private Integer bookedCount;

    private Integer totalCount;

    private BigDecimal surgeFactor;

    private BigDecimal price;

    private String city;

    private Boolean closed;
}
