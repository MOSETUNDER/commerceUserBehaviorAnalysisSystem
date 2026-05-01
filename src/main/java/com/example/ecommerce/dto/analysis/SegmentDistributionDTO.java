package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 用户分层占比/数量 DTO
 */
@Data
public class SegmentDistributionDTO {

    /**
     * 分层名称，例如：重要价值客户、一般价值客户等
     */
    private String segmentLabel;

    /**
     * 用户数量
     */
    private long count;
}


