package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 各州平均客单价 DTO
 */
@Data
public class StateAverageOrderValueDTO {
    /**
     * 州名
     */
    private String state;

    /**
     * 平均客单价
     */
    private BigDecimal averageOrderValue;
}

