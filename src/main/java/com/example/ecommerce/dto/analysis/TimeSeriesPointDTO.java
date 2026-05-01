package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 时间序列点 DTO，用于折线图/面积图
 */
@Data
public class TimeSeriesPointDTO {

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 订单数量
     */
    private long orderCount;

    /**
     * GMV（销售额）
     */
    private BigDecimal gmv;
}


