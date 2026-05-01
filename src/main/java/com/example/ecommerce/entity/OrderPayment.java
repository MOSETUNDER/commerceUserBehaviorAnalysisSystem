package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付实体类
 * 
 * @author system
 */
@Data
@TableName("order_payments")
public class OrderPayment {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 支付序号（同一订单可能有多次支付）
     */
    private Integer paymentSequential;
    
    /**
     * 支付类型（credit_card, boleto, voucher等）
     */
    private String paymentType;
    
    /**
     * 支付分期数
     */
    private Integer paymentInstallments;
    
    /**
     * 支付金额
     */
    private BigDecimal paymentValue;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

