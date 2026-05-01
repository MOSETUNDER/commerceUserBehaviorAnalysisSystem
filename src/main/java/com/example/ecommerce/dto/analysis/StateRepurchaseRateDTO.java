package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 各州复购率 DTO
 */
@Data
public class StateRepurchaseRateDTO {
    /**
     * 州名
     */
    private String state;

    /**
     * 复购率（0-1之间的小数）
     */
    private double repurchaseRate;

    /**
     * 总用户数
     */
    private long totalCustomers;

    /**
     * 复购用户数
     */
    private long repurchaseCustomers;
}

