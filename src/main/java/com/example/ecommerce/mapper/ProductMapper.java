package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品表 Mapper 接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}


