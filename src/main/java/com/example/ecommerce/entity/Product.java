package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 
 * @author system
 */
@Data
@TableName("products")
public class Product {
    
    /**
     * 商品ID
     */
    @TableId(type = IdType.INPUT)
    private String productId;
    
    /**
     * 商品类别名称
     */
    private String productCategoryName;
    
    /**
     * 商品名称长度
     */
    private Integer productNameLength;
    
    /**
     * 商品描述长度
     */
    private Integer productDescriptionLength;
    
    /**
     * 商品照片数量
     */
    private Integer productPhotosQty;
    
    /**
     * 商品重量（克）
     */
    private BigDecimal productWeightG;
    
    /**
     * 商品长度（厘米）
     */
    private BigDecimal productLengthCm;
    
    /**
     * 商品高度（厘米）
     */
    private BigDecimal productHeightCm;
    
    /**
     * 商品宽度（厘米）
     */
    private BigDecimal productWidthCm;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

