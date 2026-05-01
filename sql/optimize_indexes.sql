-- RFM 分析性能优化索引
-- 用于优化客户 RFM 数据聚合查询
USE `ecommerce_analysis`;
-- 1. 优化 orders 表的复合索引（用于 RFM 查询的 WHERE 条件和 GROUP BY）
-- 这个索引可以覆盖 WHERE 条件和 GROUP BY，大大提高查询速度
-- 如果索引已存在，会报错但可以忽略
CREATE INDEX `idx_orders_rfm_query` 
ON `orders` (`customer_id`, `order_purchase_timestamp`, `order_status`);

