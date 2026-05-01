package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体类
 * 
 * @author system
 */
@Data
@TableName("customers")
public class Customer {
    
    /**
     * 客户ID（系统内部ID）
     */
    @TableId(type = IdType.INPUT)
    private String customerId;
    
    /**
     * 客户唯一标识（真实用户ID）
     */
    private String customerUniqueId;
    
    /**
     * 客户邮编前缀
     */
    private String customerZipCodePrefix;
    
    /**
     * 客户城市
     */
    private String customerCity;
    
    /**
     * 客户州/省
     */
    private String customerState;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

