package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 用户活跃时间热力图 DTO
 * 周几 × 几点
 */
@Data
public class UserActiveTimeHeatmapDTO {
    /**
     * 周几（1-7，1=周一）
     */
    private int dayOfWeek;

    /**
     * 几点（0-23）
     */
    private int hour;

    /**
     * 订单数量
     */
    private long orderCount;
}

