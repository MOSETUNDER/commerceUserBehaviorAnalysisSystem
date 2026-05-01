package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体类
 * 
 * @author system
 */
@Data
@TableName("order_items")
public class OrderItem {
    
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
     * 订单项ID（同一订单中的商品序号）
     */
    private Integer orderItemId;
    
    /**
     * 商品ID
     */
    private String productId;
    
    /**
     * 卖家ID
     */
    private String sellerId;
    
    /**
     * 配送截止日期
     */
    private LocalDateTime shippingLimitDate;
    
    /**
     * 商品价格
     */
    private BigDecimal price;
    
    /**
     * 运费
     */
    private BigDecimal freightValue;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

