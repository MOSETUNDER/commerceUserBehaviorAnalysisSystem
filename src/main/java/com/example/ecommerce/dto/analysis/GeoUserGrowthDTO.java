package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 地域用户增长 DTO
 */
@Data
public class GeoUserGrowthDTO {
    /**
     * 州名
     */
    private String state;

    /**
     * 月份（格式：YYYY-MM）
     */
    private String month;

    /**
     * 新增用户数
     */
    private long newCustomers;
}

