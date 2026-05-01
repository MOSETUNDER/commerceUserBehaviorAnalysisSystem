package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.OrderPayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单支付表 Mapper 接口
 */
@Mapper
public interface OrderPaymentMapper extends BaseMapper<OrderPayment> {
}


