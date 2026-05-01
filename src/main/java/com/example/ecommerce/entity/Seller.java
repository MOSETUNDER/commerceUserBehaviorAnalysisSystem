package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卖家实体类
 * 
 * @author system
 */
@Data
@TableName("sellers")
public class Seller {
    
    /**
     * 卖家ID
     */
    @TableId(type = IdType.INPUT)
    private String sellerId;
    
    /**
     * 卖家邮编前缀
     */
    private String sellerZipCodePrefix;
    
    /**
     * 卖家城市
     */
    private String sellerCity;
    
    /**
     * 卖家州/省
     */
    private String sellerState;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

