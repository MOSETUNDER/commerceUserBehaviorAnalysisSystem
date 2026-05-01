package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.time.LocalDate;

/**
 * 订单量预测点 DTO
 */
@Data
public class ForecastPointDTO {

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 预测订单数量
     */
    private long predictedOrderCount;
}


