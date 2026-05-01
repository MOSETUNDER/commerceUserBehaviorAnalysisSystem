package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.Seller;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卖家表 Mapper 接口
 */
@Mapper
public interface SellerMapper extends BaseMapper<Seller> {
}


