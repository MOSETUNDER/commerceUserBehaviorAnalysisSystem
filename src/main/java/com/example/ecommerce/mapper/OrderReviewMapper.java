package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.OrderReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单评价表 Mapper 接口
 */
@Mapper
public interface OrderReviewMapper extends BaseMapper<OrderReview> {
    
    /**
     * 优化查询：评价分数分布（按评分分组统计）
     */
    @Select("SELECT " +
            "    review_score AS score, " +
            "    COUNT(*) AS count " +
            "FROM order_reviews " +
            "WHERE review_score IS NOT NULL " +
            "GROUP BY review_score " +
            "ORDER BY review_score")
    List<Map<String, Object>> aggregateReviewScoreDistribution();
}


