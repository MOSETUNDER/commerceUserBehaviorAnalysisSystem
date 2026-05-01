package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 地理维度统计 DTO：按城市/州统计用户数、订单数、GMV
 */
@Data
public class GeoStatsDTO {

    /**
     * 地理维度名称（城市或州名）
     */
    private String name;

    /**
     * 用户数量
     */
    private long customerCount;

    /**
     * 订单数量
     */
    private long orderCount;

    /**
     * GMV
     */
    private BigDecimal totalGmv;
}


