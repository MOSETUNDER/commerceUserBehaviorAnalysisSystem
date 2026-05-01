package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细表 Mapper 接口
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}


