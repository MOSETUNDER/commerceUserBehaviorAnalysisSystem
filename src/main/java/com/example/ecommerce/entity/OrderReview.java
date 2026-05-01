package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单评价实体类
 * 
 * @author system
 */
@Data
@TableName("order_reviews")
public class OrderReview {
    
    /**
     * 评价ID
     */
    @TableId(type = IdType.INPUT)
    private String reviewId;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 评价分数（1-5）
     */
    private Integer reviewScore;
    
    /**
     * 评价标题
     */
    private String reviewCommentTitle;
    
    /**
     * 评价内容
     */
    private String reviewCommentMessage;
    
    /**
     * 评价创建时间
     */
    private LocalDateTime reviewCreationDate;
    
    /**
     * 评价回复时间
     */
    private LocalDateTime reviewAnswerTimestamp;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

