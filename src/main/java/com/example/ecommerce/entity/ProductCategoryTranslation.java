package com.example.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品类别翻译实体类
 * 
 * @author system
 */
@Data
@TableName("product_category_translation")
public class ProductCategoryTranslation {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品类别名称（葡萄牙语）
     */
    private String productCategoryName;
    
    /**
     * 商品类别名称（英语）
     */
    private String productCategoryNameEnglish;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

