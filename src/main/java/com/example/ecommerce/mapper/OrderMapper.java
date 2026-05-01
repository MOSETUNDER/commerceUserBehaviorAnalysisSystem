package com.example.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 订单表 Mapper 接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    /**
     * 优化查询：直接在数据库层面聚合客户 RFM 基础数据
     * 只返回必要的聚合结果，避免加载全量数据到内存
     * 
     * 注意：使用 customer_unique_id（真实用户ID）而不是 customer_id（系统内部ID）
     * 因为同一个真实用户可能在不同时间/地点下单，系统会分配不同的 customer_id
     * 
     * 优化点：
     * 1. 使用子查询先过滤订单，减少 JOIN 的数据量
     * 2. 使用索引友好的查询条件
     * 3. 建议创建索引：idx_orders_rfm_query (customer_id, order_purchase_timestamp, order_status)
     */
    @Select("SELECT " +
            "    c.customer_unique_id AS customerUniqueId, " +
            "    MAX(DATE(o.order_purchase_timestamp)) AS lastOrderDate, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount, " +
            "    COALESCE(SUM(p.payment_value), 0) AS totalAmount " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE c.customer_unique_id IS NOT NULL " +
            "  AND o.order_purchase_timestamp IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY c.customer_unique_id " +
            "ORDER BY c.customer_unique_id")
    List<Map<String, Object>> aggregateCustomerRfmData();
    
    /**
     * 获取数据中最新的订单日期（用于确定参考日期）
     */
    @Select("SELECT MAX(DATE(order_purchase_timestamp)) AS maxDate FROM orders WHERE order_purchase_timestamp IS NOT NULL")
    LocalDate getMaxOrderDate();
    
    /**
     * 优化查询：直接在数据库层面聚合客户总消费金额
     * 用于消费金额分布直方图
     * 
     * 注意：使用 customer_unique_id（真实用户ID）而不是 customer_id（系统内部ID）
     */
    @Select("SELECT " +
            "    c.customer_unique_id AS customerUniqueId, " +
            "    COALESCE(SUM(p.payment_value), 0) AS totalAmount " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE c.customer_unique_id IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY c.customer_unique_id")
    List<Map<String, Object>> aggregateCustomerTotalAmount();
    
    /**
     * 优化查询：直接在数据库层面聚合卖家统计数据（订单数、GMV）
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    COUNT(DISTINCT oi.order_id) AS orderCount, " +
            "    COALESCE(SUM(p.payment_value), 0) AS totalGmv " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE oi.seller_id IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY oi.seller_id " +
            "ORDER BY totalGmv DESC")
    List<Map<String, Object>> aggregateSellerStats();
    
    /**
     * 优化查询：卖家-客户订单数统计（用于复购率计算）
     * 
     * 注意：使用 customer_unique_id（真实用户ID）而不是 customer_id（系统内部ID）
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    c.customer_unique_id AS customerUniqueId, " +
            "    COUNT(DISTINCT oi.order_id) AS orderCount " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "WHERE oi.seller_id IS NOT NULL " +
            "  AND c.customer_unique_id IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY oi.seller_id, c.customer_unique_id")
    List<Map<String, Object>> aggregateSellerCustomerOrderCount();
    
    /**
     * 优化查询：卖家订单量趋势（按月）
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    DATE_FORMAT(o.order_purchase_timestamp, '%Y-%m') AS month, " +
            "    COUNT(DISTINCT oi.order_id) AS orderCount " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE oi.seller_id IS NOT NULL " +
            "  AND o.order_purchase_timestamp IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY oi.seller_id, DATE_FORMAT(o.order_purchase_timestamp, '%Y-%m') " +
            "ORDER BY oi.seller_id, month")
    List<Map<String, Object>> aggregateSellerOrderTrend();
    
    /**
     * 优化查询：卖家好评率统计（直接在数据库层面计算）
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    COUNT(CASE WHEN r.review_score >= 4 THEN 1 END) AS positiveCount, " +
            "    COUNT(r.review_score) AS totalReviewed " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "LEFT JOIN order_reviews r ON o.order_id = r.order_id AND r.review_score IS NOT NULL " +
            "WHERE oi.seller_id IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY oi.seller_id")
    List<Map<String, Object>> aggregateSellerPositiveRate();
    
    /**
     * 优化查询：卖家好评率统计（带时间范围）
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    COUNT(CASE WHEN r.review_score >= 4 THEN 1 END) AS positiveCount, " +
            "    COUNT(r.review_score) AS totalReviewed " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "LEFT JOIN order_reviews r ON o.order_id = r.order_id AND r.review_score IS NOT NULL " +
            "WHERE oi.seller_id IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "  ${dateCondition} " +
            "GROUP BY oi.seller_id")
    List<Map<String, Object>> aggregateSellerPositiveRateWithDateRange(@Param("dateCondition") String dateCondition);
    
    /**
     * 优化查询：卖家统计数据（带时间范围，用于 getTopSellers）
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    COUNT(DISTINCT oi.order_id) AS orderCount, " +
            "    COALESCE(SUM(p.payment_value), 0) AS totalGmv " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE oi.seller_id IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "  ${dateCondition} " +
            "GROUP BY oi.seller_id " +
            "ORDER BY totalGmv DESC")
    List<Map<String, Object>> aggregateSellerStatsWithDateRange(@Param("dateCondition") String dateCondition);
    
    /**
     * 优化查询：Top N 卖家评分列表（用于箱线图）
     * 注意：由于 MyBatis 注解限制，这里使用字符串拼接，但只查询 Top N（通常20个），风险可控
     */
    @Select("SELECT " +
            "    oi.seller_id AS sellerId, " +
            "    r.review_score AS reviewScore " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "INNER JOIN order_reviews r ON o.order_id = r.order_id " +
            "WHERE oi.seller_id IN (${sellerIds}) " +
            "  AND r.review_score IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "ORDER BY oi.seller_id, r.review_score")
    List<Map<String, Object>> aggregateSellerReviewScores(@Param("sellerIds") String sellerIds);
    
    /**
     * 优化查询：各州销售额（用于热力图）
     */
    @Select("SELECT " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    COALESCE(SUM(p.payment_value), 0) AS sales " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY c.customer_state " +
            "ORDER BY sales DESC")
    List<Map<String, Object>> aggregateStateSales();
    
    /**
     * 优化查询：各州订单量
     */
    @Select("SELECT " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY c.customer_state " +
            "ORDER BY orderCount DESC")
    List<Map<String, Object>> aggregateStateOrderCount();
    
    /**
     * 优化查询：各州平均客单价
     */
    @Select("SELECT " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    COALESCE(AVG(p.payment_value), 0) AS averageOrderValue " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "  AND p.payment_value IS NOT NULL " +
            "GROUP BY c.customer_state " +
            "ORDER BY averageOrderValue DESC")
    List<Map<String, Object>> aggregateStateAverageOrderValue();
    
    /**
     * 优化查询：各州复购率（复购用户 = 订单数 >= 2 的用户）
     * 
     * 注意：使用 customer_unique_id（真实用户ID）而不是 customer_id（系统内部ID）
     */
    @Select("SELECT " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    COUNT(DISTINCT c.customer_unique_id) AS totalCustomers, " +
            "    COUNT(DISTINCT CASE WHEN orderCount.orderCount >= 2 THEN c.customer_unique_id END) AS repurchaseCustomers " +
            "FROM customers c " +
            "LEFT JOIN ( " +
            "    SELECT c2.customer_unique_id, COUNT(DISTINCT o.order_id) AS orderCount " +
            "    FROM orders o " +
            "    INNER JOIN customers c2 ON o.customer_id = c2.customer_id " +
            "    WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "    GROUP BY c2.customer_unique_id " +
            ") orderCount ON c.customer_unique_id = orderCount.customer_unique_id " +
            "WHERE c.customer_state IS NOT NULL " +
            "GROUP BY c.customer_state " +
            "ORDER BY repurchaseCustomers DESC")
    List<Map<String, Object>> aggregateStateRepurchaseRate();
    
    /**
     * 优化查询：各州物流时效（平均物流天数）
     */
    @Select("SELECT " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    AVG(DATEDIFF(o.order_delivered_customer_date, o.order_purchase_timestamp)) AS averageDeliveryDays, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "  AND o.order_delivered_customer_date IS NOT NULL " +
            "  AND o.order_purchase_timestamp IS NOT NULL " +
            "GROUP BY c.customer_state " +
            "ORDER BY averageDeliveryDays ASC")
    List<Map<String, Object>> aggregateStateLogisticsEfficiency();
    
    /**
     * 优化查询：地域用户增长（按月统计）
     */
    @Select("SELECT " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    DATE_FORMAT(c.create_time, '%Y-%m') AS month, " +
            "    COUNT(DISTINCT c.customer_id) AS newCustomers " +
            "FROM customers c " +
            "WHERE c.create_time IS NOT NULL " +
            "  AND c.customer_state IS NOT NULL " +
            "GROUP BY c.customer_state, DATE_FORMAT(c.create_time, '%Y-%m') " +
            "ORDER BY c.customer_state, month")
    List<Map<String, Object>> aggregateGeoUserGrowth();
    
    /**
     * 优化查询：Top N 品类销售额
     * 注意：MySQL 的 LIMIT 不支持参数化，这里先查询 Top 50 然后在代码中限制
     */
    @Select("SELECT " +
            "    COALESCE(p.product_category_name, '未知品类') AS categoryName, " +
            "    COUNT(DISTINCT oi.order_id) AS orderCount, " +
            "    SUM(oi.price + oi.freight_value) AS salesAmount " +
            "FROM order_items oi " +
            "INNER JOIN products p ON oi.product_id = p.product_id " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY p.product_category_name " +
            "ORDER BY salesAmount DESC " +
            "LIMIT 50")
    List<Map<String, Object>> aggregateTopCategorySales();
    
    /**
     * 优化查询：品类复购率热力图（品类 × 用户分层）
     * 需要关联 RFM 分层数据
     * 
     * 注意：使用 customer_unique_id（真实用户ID）而不是 customer_id（系统内部ID）
     */
    @Select("SELECT " +
            "    COALESCE(p.product_category_name, '未知品类') AS categoryName, " +
            "    CASE " +
            "        WHEN customerOrderCount.orderCount >= 2 THEN '高价值客户' " +
            "        WHEN customerOrderCount.orderCount = 1 THEN '一般客户' " +
            "        ELSE '流失客户' " +
            "    END AS segmentLabel, " +
            "    COUNT(DISTINCT CASE WHEN customerOrderCount.orderCount >= 2 THEN c.customer_unique_id END) AS repurchaseCustomers, " +
            "    COUNT(DISTINCT c.customer_unique_id) AS totalCustomers " +
            "FROM order_items oi " +
            "INNER JOIN products p ON oi.product_id = p.product_id " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "LEFT JOIN ( " +
            "    SELECT c2.customer_unique_id, COUNT(DISTINCT o2.order_id) AS orderCount " +
            "    FROM orders o2 " +
            "    INNER JOIN customers c2 ON o2.customer_id = c2.customer_id " +
            "    WHERE (o2.order_status IS NULL OR o2.order_status != 'cancelled') " +
            "    GROUP BY c2.customer_unique_id " +
            ") customerOrderCount ON c.customer_unique_id = customerOrderCount.customer_unique_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "  AND p.product_category_name IS NOT NULL " +
            "GROUP BY p.product_category_name, segmentLabel " +
            "ORDER BY categoryName, segmentLabel")
    List<Map<String, Object>> aggregateCategoryRepurchaseHeatmap();
    
    /**
     * 优化查询：Top N 商品销售（销售量、销售额）
     * 注意：MySQL 的 LIMIT 不支持参数化，这里先查询 Top 100 然后在代码中限制
     */
    @Select("SELECT " +
            "    oi.product_id AS productId, " +
            "    COALESCE(p.product_category_name, '未知品类') AS categoryName, " +
            "    COUNT(*) AS salesCount, " +
            "    SUM(oi.price + oi.freight_value) AS salesAmount " +
            "FROM order_items oi " +
            "INNER JOIN products p ON oi.product_id = p.product_id " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY oi.product_id, p.product_category_name " +
            "ORDER BY salesAmount DESC " +
            "LIMIT 100")
    List<Map<String, Object>> aggregateTopProductSales();
    
    /**
     * 优化查询：品类各州销售地图
     */
    @Select("SELECT " +
            "    COALESCE(p.product_category_name, '未知品类') AS categoryName, " +
            "    COALESCE(c.customer_state, '未知州省') AS state, " +
            "    COUNT(DISTINCT oi.order_id) AS orderCount, " +
            "    SUM(oi.price + oi.freight_value) AS salesAmount " +
            "FROM order_items oi " +
            "INNER JOIN products p ON oi.product_id = p.product_id " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "  AND p.product_category_name IS NOT NULL " +
            "GROUP BY p.product_category_name, c.customer_state " +
            "ORDER BY categoryName, salesAmount DESC")
    List<Map<String, Object>> aggregateCategoryStateSales();
    
    /**
     * 优化查询：用户活跃时间热力图（周几 × 几点）
     */
    @Select("SELECT " +
            "    DAYOFWEEK(o.order_purchase_timestamp) AS dayOfWeek, " +
            "    HOUR(o.order_purchase_timestamp) AS hour, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount " +
            "FROM orders o " +
            "WHERE o.order_purchase_timestamp IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY DAYOFWEEK(o.order_purchase_timestamp), HOUR(o.order_purchase_timestamp) " +
            "ORDER BY dayOfWeek, hour")
    List<Map<String, Object>> aggregateUserActiveTime();
    
    /**
     * 优化查询：订单状态统计（用于漏斗图）
     */
    @Select("SELECT " +
            "    COUNT(*) AS totalOrders, " +
            "    COUNT(CASE WHEN o.order_approved_at IS NOT NULL THEN 1 END) AS approvedOrders, " +
            "    COUNT(CASE WHEN o.order_delivered_carrier_date IS NOT NULL THEN 1 END) AS shippedOrders, " +
            "    COUNT(CASE WHEN o.order_delivered_customer_date IS NOT NULL THEN 1 END) AS deliveredOrders " +
            "FROM orders o " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled')")
    Map<String, Object> aggregateOrderFunnel();
    
    /**
     * 优化查询：用户购买次数分布（用于桑基图）
     * 注意：使用 customer_unique_id（真实用户ID）而不是 customer_id（系统内部ID）
     * 因为同一个真实用户可能在不同时间/地点下单，系统会分配不同的 customer_id
     */
    @Select("SELECT " +
            "    c.customer_unique_id AS customerUniqueId, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount " +
            "FROM orders o " +
            "INNER JOIN customers c ON o.customer_id = c.customer_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY c.customer_unique_id")
    List<Map<String, Object>> aggregateCustomerOrderCount();
    
    /**
     * 优化查询：运营指标概览（客户数、订单数、有效订单数、GMV、客单价）
     */
    @Select("SELECT " +
            "    (SELECT COUNT(DISTINCT customer_unique_id) FROM customers) AS customerCount, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount, " +
            "    COUNT(DISTINCT CASE WHEN o.order_status = 'delivered' THEN o.order_id END) AS validOrderCount, " +
            "    COALESCE(SUM(p.payment_value), 0) AS totalGmv " +
            "FROM orders o " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE (o.order_status IS NULL OR o.order_status != 'cancelled')")
    Map<String, Object> aggregateKpiOverview();
    
    /**
     * 优化查询：订单趋势（按日期分组统计订单数和GMV）
     */
    @Select("SELECT " +
            "    DATE(o.order_purchase_timestamp) AS date, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount, " +
            "    COALESCE(SUM(p.payment_value), 0) AS gmv " +
            "FROM orders o " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE o.order_purchase_timestamp IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY DATE(o.order_purchase_timestamp) " +
            "ORDER BY date")
    List<Map<String, Object>> aggregateOrderTrend();
    
    /**
     * 优化查询：客单价趋势（按月统计）
     */
    @Select("SELECT " +
            "    DATE_FORMAT(o.order_purchase_timestamp, '%Y-%m') AS month, " +
            "    COUNT(DISTINCT o.order_id) AS orderCount, " +
            "    COALESCE(SUM(p.payment_value), 0) AS totalGmv, " +
            "    CASE " +
            "        WHEN COUNT(DISTINCT o.order_id) > 0 THEN COALESCE(SUM(p.payment_value), 0) / COUNT(DISTINCT o.order_id) " +
            "        ELSE 0 " +
            "    END AS averageOrderValue " +
            "FROM orders o " +
            "LEFT JOIN order_payments p ON o.order_id = p.order_id " +
            "WHERE o.order_purchase_timestamp IS NOT NULL " +
            "  AND (o.order_status IS NULL OR o.order_status != 'cancelled') " +
            "GROUP BY DATE_FORMAT(o.order_purchase_timestamp, '%Y-%m') " +
            "ORDER BY month")
    List<Map<String, Object>> aggregateAverageOrderValueTrend();
}


