package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 各州订单量 DTO
 */
@Data
public class StateOrderCountDTO {
    /**
     * 州名
     */
    private String state;

    /**
     * 订单数量
     */
    private long orderCount;
}

