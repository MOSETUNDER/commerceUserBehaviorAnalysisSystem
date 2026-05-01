package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 品类复购率热力图 DTO
 * 品类 × 用户分层
 */
@Data
public class CategoryRepurchaseHeatmapDTO {
    /**
     * 品类名称
     */
    private String categoryName;

    /**
     * 用户分层（高价值、潜力、一般、流失）
     */
    private String segmentLabel;

    /**
     * 复购率（0-1之间的小数）
     */
    private double repurchaseRate;

    /**
     * 该品类该分层的用户数
     */
    private long customerCount;
}

