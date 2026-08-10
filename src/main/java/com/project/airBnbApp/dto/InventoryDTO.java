package com.project.airBnbApp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class InventoryDTO {

    private Long id;

    private Date date;

    private Integer bookedCount;

    private Integer reservedCount;

    private Integer totalCount;

    private BigDecimal surgeFactor;

    private BigDecimal price;

    private Boolean closed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
