package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户表 Mapper 接口
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}


