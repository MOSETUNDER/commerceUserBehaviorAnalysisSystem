package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 用户活跃 / 沉睡 / 流失 统计 DTO
 */
@Data
public class ChurnStatsDTO {

    /**
     * 活跃用户数（最近 N 天有下单）
     */
    private long activeCount;

    /**
     * 沉睡用户数（最近 M-N 天有下单）
     */
    private long sleepingCount;

    /**
     * 流失用户数（超过 M 天无下单）
     */
    private long churnedCount;
}


