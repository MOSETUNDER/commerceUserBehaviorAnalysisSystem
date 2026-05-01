package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体类
 * 
 * @author system
 */
@Data
@TableName("orders")
public class Order {
    
    /**
     * 订单ID
     */
    @TableId(type = IdType.INPUT)
    private String orderId;
    
    /**
     * 客户ID
     */
    private String customerId;
    
    /**
     * 订单状态（delivered, shipped, canceled等）
     */
    private String orderStatus;
    
    /**
     * 订单购买时间
     */
    private LocalDateTime orderPurchaseTimestamp;
    
    /**
     * 订单批准时间
     */
    private LocalDateTime orderApprovedAt;
    
    /**
     * 订单交付给承运商时间
     */
    private LocalDateTime orderDeliveredCarrierDate;
    
    /**
     * 订单交付给客户时间
     */
    private LocalDateTime orderDeliveredCustomerDate;
    
    /**
     * 订单预计交付时间
     */
    private LocalDateTime orderEstimatedDeliveryDate;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

