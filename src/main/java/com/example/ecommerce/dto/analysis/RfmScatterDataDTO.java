package com.example.ecommerce.dto.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * RFM 散点图数据 DTO
 * 用于展示用户价值分布：X轴=Recency, Y轴=Frequency, 颜色/大小=Monetary
 */
@Data
public class RfmScatterDataDTO {

    /**
     * 客户ID
     */
    private String customerId;

    /**
     * R 分数（X轴）
     */
    @JsonProperty("rScore")
    private int rScore;

    /**
     * F 分数（Y轴）
     */
    @JsonProperty("fScore")
    private int fScore;

    /**
     * M 分数（用于颜色和大小）
     */
    @JsonProperty("mScore")
    private int mScore;

    /**
     * 累计消费金额（Monetary）
     */
    private BigDecimal totalAmount;

    /**
     * 用户分层标签
     */
    private String segmentLabel;
}

