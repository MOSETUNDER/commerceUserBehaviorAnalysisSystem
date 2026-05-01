-- 跨境电商用户消费行为分析系统数据库表结构
-- 基于Olist数据集设计

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ecommerce_analysis` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `ecommerce_analysis`;

-- 1. 客户表
CREATE TABLE IF NOT EXISTS `customers` (
  `customer_id` VARCHAR(50) NOT NULL COMMENT '客户ID（系统内部ID）',
  `customer_unique_id` VARCHAR(50) NOT NULL COMMENT '客户唯一标识（真实用户ID）',
  `customer_zip_code_prefix` VARCHAR(10) DEFAULT NULL COMMENT '客户邮编前缀',
  `customer_city` VARCHAR(100) DEFAULT NULL COMMENT '客户城市',
  `customer_state` VARCHAR(10) DEFAULT NULL COMMENT '客户州/省',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`customer_id`),
  KEY `idx_customer_unique_id` (`customer_unique_id`),
  KEY `idx_customer_state` (`customer_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';

-- 2. 卖家表
CREATE TABLE IF NOT EXISTS `sellers` (
  `seller_id` VARCHAR(50) NOT NULL COMMENT '卖家ID',
  `seller_zip_code_prefix` VARCHAR(10) DEFAULT NULL COMMENT '卖家邮编前缀',
  `seller_city` VARCHAR(100) DEFAULT NULL COMMENT '卖家城市',
  `seller_state` VARCHAR(10) DEFAULT NULL COMMENT '卖家州/省',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`seller_id`),
  KEY `idx_seller_state` (`seller_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卖家表';

-- 3. 商品类别翻译表
CREATE TABLE IF NOT EXISTS `product_category_translation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_category_name` VARCHAR(100) DEFAULT NULL COMMENT '商品类别名称（葡萄牙语）',
  `product_category_name_english` VARCHAR(100) DEFAULT NULL COMMENT '商品类别名称（英语）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`product_category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品类别翻译表';

-- 4. 商品表
CREATE TABLE IF NOT EXISTS `products` (
  `product_id` VARCHAR(50) NOT NULL COMMENT '商品ID',
  `product_category_name` VARCHAR(100) DEFAULT NULL COMMENT '商品类别名称',
  `product_name_length` INT DEFAULT NULL COMMENT '商品名称长度',
  `product_description_length` INT DEFAULT NULL COMMENT '商品描述长度',
  `product_photos_qty` INT DEFAULT NULL COMMENT '商品照片数量',
  `product_weight_g` DECIMAL(10,2) DEFAULT NULL COMMENT '商品重量（克）',
  `product_length_cm` DECIMAL(10,2) DEFAULT NULL COMMENT '商品长度（厘米）',
  `product_height_cm` DECIMAL(10,2) DEFAULT NULL COMMENT '商品高度（厘米）',
  `product_width_cm` DECIMAL(10,2) DEFAULT NULL COMMENT '商品宽度（厘米）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`product_id`),
  KEY `idx_product_category` (`product_category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 5. 订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` VARCHAR(50) NOT NULL COMMENT '订单ID',
  `customer_id` VARCHAR(50) NOT NULL COMMENT '客户ID',
  `order_status` VARCHAR(20) DEFAULT NULL COMMENT '订单状态（delivered, shipped, canceled等）',
  `order_purchase_timestamp` DATETIME DEFAULT NULL COMMENT '订单购买时间',
  `order_approved_at` DATETIME DEFAULT NULL COMMENT '订单批准时间',
  `order_delivered_carrier_date` DATETIME DEFAULT NULL COMMENT '订单交付给承运商时间',
  `order_delivered_customer_date` DATETIME DEFAULT NULL COMMENT '订单交付给客户时间',
  `order_estimated_delivery_date` DATETIME DEFAULT NULL COMMENT '订单预计交付时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_order_purchase_timestamp` (`order_purchase_timestamp`),
  CONSTRAINT `fk_orders_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 6. 订单明细表
CREATE TABLE IF NOT EXISTS `order_items` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` VARCHAR(50) NOT NULL COMMENT '订单ID',
  `order_item_id` INT NOT NULL COMMENT '订单项ID（同一订单中的商品序号）',
  `product_id` VARCHAR(50) NOT NULL COMMENT '商品ID',
  `seller_id` VARCHAR(50) NOT NULL COMMENT '卖家ID',
  `shipping_limit_date` DATETIME DEFAULT NULL COMMENT '配送截止日期',
  `price` DECIMAL(10,2) DEFAULT NULL COMMENT '商品价格',
  `freight_value` DECIMAL(10,2) DEFAULT NULL COMMENT '运费',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_seller_id` (`seller_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `fk_order_items_seller` FOREIGN KEY (`seller_id`) REFERENCES `sellers` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- 7. 订单支付表
CREATE TABLE IF NOT EXISTS `order_payments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` VARCHAR(50) NOT NULL COMMENT '订单ID',
  `payment_sequential` INT DEFAULT NULL COMMENT '支付序号（同一订单可能有多次支付）',
  `payment_type` VARCHAR(50) DEFAULT NULL COMMENT '支付类型（credit_card, boleto, voucher等）',
  `payment_installments` INT DEFAULT NULL COMMENT '支付分期数',
  `payment_value` DECIMAL(10,2) DEFAULT NULL COMMENT '支付金额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_payment_type` (`payment_type`),
  CONSTRAINT `fk_order_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单支付表';

-- 8. 订单评价表
CREATE TABLE IF NOT EXISTS `order_reviews` (
  `review_id` VARCHAR(50) NOT NULL COMMENT '评价ID',
  `order_id` VARCHAR(50) NOT NULL COMMENT '订单ID',
  `review_score` INT DEFAULT NULL COMMENT '评价分数（1-5）',
  `review_comment_title` VARCHAR(500) DEFAULT NULL COMMENT '评价标题',
  `review_comment_message` TEXT DEFAULT NULL COMMENT '评价内容',
  `review_creation_date` DATETIME DEFAULT NULL COMMENT '评价创建时间',
  `review_answer_timestamp` DATETIME DEFAULT NULL COMMENT '评价回复时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`review_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_review_score` (`review_score`),
  CONSTRAINT `fk_order_reviews_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单评价表';

