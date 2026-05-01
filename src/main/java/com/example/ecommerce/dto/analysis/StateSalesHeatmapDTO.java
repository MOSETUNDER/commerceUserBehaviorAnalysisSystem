package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 各州销售额热力图 DTO
 */
@Data
public class StateSalesHeatmapDTO {
    /**
     * 州名
     */
    private String state;

    /**
     * 销售额
     */
    private BigDecimal sales;
}

