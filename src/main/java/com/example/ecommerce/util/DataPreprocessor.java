package com.example.ecommerce.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据预处理工具类
 * 功能：
 * 1. 数据清洗（去除无效订单、空值处理等）
 * 2. 数据转换（日期格式、金额计算等）
 * 3. 数据聚合（按客户聚合订单数据）
 * 4. 数据验证（数据完整性检查）
 */
public class DataPreprocessor {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ARCHIVE_PATH = "archive/";
    
    // 统计信息
    private static int totalOrders = 0;
    private static int validOrders = 0;
    private static int invalidOrders = 0;
    private static int totalCustomers = 0;
    private static int validCustomers = 0;
    private static int totalOrderItems = 0;
    private static int validOrderItems = 0;
    private static int totalPayments = 0;
    private static int validPayments = 0;
    
    // 验证结果
    private static final List<String> validationErrors = new ArrayList<>();
    private static final List<String> validationWarnings = new ArrayList<>();

    public static void main(String[] args) {
        try {
            System.out.println("==========================================");
            System.out.println("开始数据预处理...");
            System.out.println("==========================================");
            
            // 1. 数据清洗和转换
            System.out.println("\n【步骤1】数据清洗和转换...");
            Map<String, String> customerMap = loadAndCleanCustomers();
            Map<String, Double> orderAmountMap = loadAndCleanOrderItems();
            List<OrderRecord> orders = loadAndCleanOrders(customerMap, orderAmountMap);
            List<PaymentRecord> payments = loadAndCleanPayments();
            List<ReviewRecord> reviews = loadAndCleanReviews();
            
            // 2. 数据验证
            System.out.println("\n【步骤2】数据验证...");
            validateData(orders, payments, reviews, customerMap);
            
            // 3. 数据聚合
            System.out.println("\n【步骤3】数据聚合...");
            Map<String, CustomerAggregate> aggregated = aggregateCustomerData(orders);
            
            // 4. 输出处理后的数据
            System.out.println("\n【步骤4】输出处理后的数据...");
            outputProcessedData(orders, payments, reviews, aggregated);
            
            // 5. 输出统计报告
            System.out.println("\n【步骤5】生成统计报告...");
            printStatistics();
            
            System.out.println("\n==========================================");
            System.out.println("数据预处理完成！");
            System.out.println("==========================================");
            
        } catch (Exception e) {
            System.err.println("数据预处理失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 1. 加载并清洗客户数据
     */
    private static Map<String, String> loadAndCleanCustomers() throws Exception {
        System.out.println("  处理客户数据...");
        Map<String, String> customerMap = new HashMap<>();
        Map<String, CustomerInfo> customerInfoMap = new HashMap<>();
        
        File file = new File(ARCHIVE_PATH + "olist_customers_dataset.csv");
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + file.getAbsolutePath());
        }
        
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withSkipLines(1).build()) {
            String[] line;
            int lineNum = 2; // 从第2行开始（跳过表头）
            
            while ((line = reader.readNext()) != null) {
                totalCustomers++;
                lineNum++;
                
                if (line.length < 5) {
                    validationErrors.add(String.format("客户数据第%d行：字段数量不足", lineNum));
                    invalidOrders++;
                    continue;
                }
                
                String customerId = cleanString(line[0]);
                String customerUniqueId = cleanString(line[1]);
                String zipCodePrefix = cleanString(line[2]);
                String city = cleanString(line[3]);
                String state = cleanString(line[4]);
                
                // 验证必填字段
                if (customerId == null || customerId.isEmpty()) {
                    validationErrors.add(String.format("客户数据第%d行：customer_id 为空", lineNum));
                    continue;
                }
                if (customerUniqueId == null || customerUniqueId.isEmpty()) {
                    validationErrors.add(String.format("客户数据第%d行：customer_unique_id 为空", lineNum));
                    continue;
                }
                
                // 数据转换：处理空值和默认值
                if (zipCodePrefix == null || zipCodePrefix.isEmpty()) {
                    zipCodePrefix = "00000";
                    validationWarnings.add(String.format("客户数据第%d行：zip_code_prefix 为空，使用默认值", lineNum));
                }
                if (city == null || city.isEmpty()) {
                    city = "未知城市";
                    validationWarnings.add(String.format("客户数据第%d行：city 为空，使用默认值", lineNum));
                }
                if (state == null || state.isEmpty()) {
                    state = "未知州省";
                    validationWarnings.add(String.format("客户数据第%d行：state 为空，使用默认值", lineNum));
                }
                
                customerMap.put(customerId, customerUniqueId);
                
                CustomerInfo info = new CustomerInfo();
                info.customerId = customerId;
                info.customerUniqueId = customerUniqueId;
                info.zipCodePrefix = zipCodePrefix;
                info.city = city;
                info.state = state;
                customerInfoMap.put(customerUniqueId, info);
                
                validCustomers++;
            }
        }
        
        // 输出清洗后的客户数据
        outputCustomers(customerInfoMap);
        
        System.out.println("    总客户数: " + totalCustomers);
        System.out.println("    有效客户数: " + validCustomers);
        System.out.println("    无效客户数: " + (totalCustomers - validCustomers));
        
        return customerMap;
    }

    /**
     * 2. 加载并清洗订单明细数据
     */
    private static Map<String, Double> loadAndCleanOrderItems() throws Exception {
        System.out.println("  处理订单明细数据...");
        Map<String, Double> orderAmountMap = new HashMap<>();
        
        File file = new File(ARCHIVE_PATH + "olist_order_items_dataset.csv");
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + file.getAbsolutePath());
        }
        
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withSkipLines(1).build()) {
            String[] line;
            int lineNum = 2;
            
            while ((line = reader.readNext()) != null) {
                totalOrderItems++;
                lineNum++;
                
                if (line.length < 7) {
                    validationErrors.add(String.format("订单明细第%d行：字段数量不足", lineNum));
                    continue;
                }
                
                String orderId = cleanString(line[0]);
                String productId = cleanString(line[1]);
                String sellerId = cleanString(line[2]);
                
                // 验证必填字段
                if (orderId == null || orderId.isEmpty()) {
                    validationErrors.add(String.format("订单明细第%d行：order_id 为空", lineNum));
                    continue;
                }
                
                // 数据转换：金额计算（price + freight_value）
                double price = safeParseDouble(line[5], 0.0);
                double freightValue = safeParseDouble(line[6], 0.0);
                double totalAmount = price + freightValue;
                
                // 数据验证：金额必须大于0
                if (totalAmount <= 0) {
                    validationWarnings.add(String.format("订单明细第%d行：订单 %s 金额为0或负数，跳过", lineNum, orderId));
                    continue;
                }
                
                orderAmountMap.merge(orderId, totalAmount, Double::sum);
                validOrderItems++;
            }
        }
        
        System.out.println("    总订单明细数: " + totalOrderItems);
        System.out.println("    有效订单明细数: " + validOrderItems);
        System.out.println("    无效订单明细数: " + (totalOrderItems - validOrderItems));
        
        return orderAmountMap;
    }

    /**
     * 3. 加载并清洗订单数据
     */
    private static List<OrderRecord> loadAndCleanOrders(Map<String, String> customerMap, 
                                                         Map<String, Double> orderAmountMap) throws Exception {
        System.out.println("  处理订单数据...");
        List<OrderRecord> orders = new ArrayList<>();
        
        File file = new File(ARCHIVE_PATH + "olist_orders_dataset.csv");
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + file.getAbsolutePath());
        }
        
        // 统计各种无效原因
        int cancelledCount = 0;
        int missingOrderIdCount = 0;
        int missingCustomerIdCount = 0;
        int customerNotFoundCount = 0;
        int missingAmountCount = 0;
        int invalidDateCount = 0;
        
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withSkipLines(1).build()) {
            String[] line;
            int lineNum = 2;
            
            while ((line = reader.readNext()) != null) {
                totalOrders++;
                lineNum++;
                
                if (line.length < 9) {
                    validationErrors.add(String.format("订单数据第%d行：字段数量不足（%d个字段）", lineNum, line.length));
                    invalidOrders++;
                    continue;
                }
                
                String orderId = cleanString(line[0]);
                String customerId = cleanString(line[1]);
                String orderStatus = cleanString(line[3]);
                String purchaseTimestamp = cleanString(line[4]);
                String approvedAt = cleanString(line[5]);
                String deliveredCarrierDate = cleanString(line[6]);
                String deliveredCustomerDate = cleanString(line[7]);
                String estimatedDeliveryDate = cleanString(line[8]);
                
                // 数据清洗：去除无效订单（已取消的订单）
                if ("cancelled".equalsIgnoreCase(orderStatus)) {
                    cancelledCount++;
                    invalidOrders++;
                    continue;
                }
                
                // 验证必填字段
                if (orderId == null || orderId.isEmpty()) {
                    missingOrderIdCount++;
                    validationErrors.add(String.format("订单数据第%d行：order_id 为空", lineNum));
                    invalidOrders++;
                    continue;
                }
                if (customerId == null || customerId.isEmpty()) {
                    missingCustomerIdCount++;
                    validationErrors.add(String.format("订单数据第%d行：customer_id 为空", lineNum));
                    invalidOrders++;
                    continue;
                }
                
                // 验证客户ID映射
                String customerUniqueId = customerMap.get(customerId);
                if (customerUniqueId == null) {
                    customerNotFoundCount++;
                    if (customerNotFoundCount <= 5) { // 只记录前5个，避免日志过多
                        validationWarnings.add(String.format("订单数据第%d行：订单 %s 的客户ID %s 不存在于客户表中", 
                                lineNum, orderId, customerId));
                    }
                    invalidOrders++;
                    continue;
                }
                
                // 验证订单金额
                Double orderAmount = orderAmountMap.get(orderId);
                if (orderAmount == null || orderAmount <= 0) {
                    missingAmountCount++;
                    if (missingAmountCount <= 5) { // 只记录前5个
                        validationWarnings.add(String.format("订单数据第%d行：订单 %s 没有有效的金额数据", lineNum, orderId));
                    }
                    invalidOrders++;
                    continue;
                }
                
                // 数据转换：日期格式转换
                LocalDateTime purchaseTime = parseDateTime(purchaseTimestamp);
                if (purchaseTime == null) {
                    invalidDateCount++;
                    if (invalidDateCount <= 5) { // 只记录前5个
                        validationErrors.add(String.format("订单数据第%d行：订单 %s 的 purchase_timestamp 格式错误: %s", 
                                lineNum, orderId, purchaseTimestamp));
                    }
                    invalidOrders++;
                    continue;
                }
                
                LocalDateTime approvedTime = parseDateTime(approvedAt);
                LocalDateTime deliveredCarrierTime = parseDateTime(deliveredCarrierDate);
                LocalDateTime deliveredCustomerTime = parseDateTime(deliveredCustomerDate);
                LocalDateTime estimatedDeliveryTime = parseDateTime(estimatedDeliveryDate);
                
                // 数据验证：订单状态逻辑验证
                if (orderStatus == null || orderStatus.isEmpty()) {
                    orderStatus = "unknown";
                    validationWarnings.add(String.format("订单数据第%d行：订单 %s 状态为空，使用默认值", lineNum, orderId));
                }
                
                OrderRecord order = new OrderRecord();
                order.orderId = orderId;
                order.customerId = customerId;
                order.customerUniqueId = customerUniqueId;
                order.orderStatus = orderStatus;
                order.purchaseTimestamp = purchaseTime;
                order.approvedAt = approvedTime;
                order.deliveredCarrierDate = deliveredCarrierTime;
                order.deliveredCustomerDate = deliveredCustomerTime;
                order.estimatedDeliveryDate = estimatedDeliveryTime;
                order.totalAmount = orderAmount;
                
                orders.add(order);
                validOrders++;
            }
        }
        
        System.out.println("    总订单数: " + totalOrders);
        System.out.println("    有效订单数: " + validOrders);
        System.out.println("    无效订单数: " + invalidOrders);
        if (invalidOrders > 0) {
            System.out.println("    无效原因统计：");
            System.out.println("      已取消订单: " + cancelledCount);
            System.out.println("      缺少订单ID: " + missingOrderIdCount);
            System.out.println("      缺少客户ID: " + missingCustomerIdCount);
            System.out.println("      客户ID不存在: " + customerNotFoundCount);
            System.out.println("      缺少订单金额: " + missingAmountCount);
            System.out.println("      日期格式错误: " + invalidDateCount);
        }
        
        return orders;
    }

    /**
     * 4. 加载并清洗支付数据
     */
    private static List<PaymentRecord> loadAndCleanPayments() throws Exception {
        System.out.println("  处理支付数据...");
        List<PaymentRecord> payments = new ArrayList<>();
        
        File file = new File(ARCHIVE_PATH + "olist_order_payments_dataset.csv");
        if (!file.exists()) {
            System.out.println("    警告：支付数据文件不存在，跳过");
            return payments;
        }
        
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withSkipLines(1).build()) {
            String[] line;
            int lineNum = 2;
            
            while ((line = reader.readNext()) != null) {
                totalPayments++;
                lineNum++;
                
                if (line.length < 5) {
                    validationErrors.add(String.format("支付数据第%d行：字段数量不足", lineNum));
                    continue;
                }
                
                String orderId = cleanString(line[0]);
                String paymentType = cleanString(line[1]);
                int installments = safeParseInt(line[2], 1);
                double paymentValue = safeParseDouble(line[3], 0.0);
                
                // 验证必填字段
                if (orderId == null || orderId.isEmpty()) {
                    validationErrors.add(String.format("支付数据第%d行：order_id 为空", lineNum));
                    continue;
                }
                
                // 数据验证：支付金额必须大于0
                if (paymentValue <= 0) {
                    validationWarnings.add(String.format("支付数据第%d行：订单 %s 支付金额为0或负数，跳过", lineNum, orderId));
                    continue;
                }
                
                // 数据转换：处理空值
                if (paymentType == null || paymentType.isEmpty()) {
                    paymentType = "unknown";
                    validationWarnings.add(String.format("支付数据第%d行：payment_type 为空，使用默认值", lineNum));
                }
                if (installments <= 0) {
                    installments = 1;
                    validationWarnings.add(String.format("支付数据第%d行：installments 无效，使用默认值1", lineNum));
                }
                
                PaymentRecord payment = new PaymentRecord();
                payment.orderId = orderId;
                payment.paymentType = paymentType;
                payment.installments = installments;
                payment.paymentValue = paymentValue;
                
                payments.add(payment);
                validPayments++;
            }
        }
        
        System.out.println("    总支付记录数: " + totalPayments);
        System.out.println("    有效支付记录数: " + validPayments);
        System.out.println("    无效支付记录数: " + (totalPayments - validPayments));
        
        return payments;
    }

    /**
     * 5. 加载并清洗评价数据
     */
    private static List<ReviewRecord> loadAndCleanReviews() throws Exception {
        System.out.println("  处理评价数据...");
        List<ReviewRecord> reviews = new ArrayList<>();
        
        File file = new File(ARCHIVE_PATH + "olist_order_reviews_dataset.csv");
        if (!file.exists()) {
            System.out.println("    警告：评价数据文件不存在，跳过");
            return reviews;
        }
        
        // 使用 RFC4180Parser 来处理包含换行符的字段
        com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder()
                .withSeparator(',')
                .withQuoteChar('"')
                .withEscapeChar('\\')
                .withStrictQuotes(false)
                .withIgnoreLeadingWhiteSpace(true)
                .build();
        
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withSkipLines(1)
                .withCSVParser(parser)
                .build()) {
            String[] line;
            int lineNum = 2;
            
            while ((line = reader.readNext()) != null) {
                try {
                    if (line.length < 7) {
                        validationWarnings.add(String.format("评价数据第%d行：字段数量不足（%d个字段）", lineNum, line.length));
                        lineNum++;
                        continue;
                    }
                    
                    String reviewId = cleanString(line[0]);
                    String orderId = cleanString(line[1]);
                    int reviewScore = safeParseInt(line[2], 0);
                    String reviewCommentTitle = cleanString(line[4]);
                    String reviewCommentMessage = cleanString(line[5]);
                    String reviewCreationDate = cleanString(line[6]);
                    
                    // 验证必填字段
                    if (orderId == null || orderId.isEmpty()) {
                        validationWarnings.add(String.format("评价数据第%d行：order_id 为空，跳过", lineNum));
                        lineNum++;
                        continue;
                    }
                    
                    // 数据验证：评分必须在1-5之间
                    if (reviewScore < 1 || reviewScore > 5) {
                        validationWarnings.add(String.format("评价数据第%d行：订单 %s 评分 %d 无效，跳过", 
                                lineNum, orderId, reviewScore));
                        lineNum++;
                        continue;
                    }
                    
                    // 数据转换：日期格式转换
                    LocalDateTime creationDate = parseDateTime(reviewCreationDate);
                    
                    ReviewRecord review = new ReviewRecord();
                    review.reviewId = reviewId != null ? reviewId : "";
                    review.orderId = orderId;
                    review.reviewScore = reviewScore;
                    review.reviewCommentTitle = reviewCommentTitle != null ? reviewCommentTitle : "";
                    review.reviewCommentMessage = reviewCommentMessage != null ? reviewCommentMessage : "";
                    review.reviewCreationDate = creationDate;
                    
                    reviews.add(review);
                } catch (Exception e) {
                    validationWarnings.add(String.format("评价数据第%d行：解析失败 - %s", lineNum, e.getMessage()));
                }
                lineNum++;
            }
        } catch (Exception e) {
            System.err.println("    警告：评价数据解析出错，已处理 " + reviews.size() + " 条记录");
            System.err.println("    错误信息: " + e.getMessage());
            // 继续执行，不中断整个流程
        }
        
        System.out.println("    总评价记录数: " + reviews.size());
        
        return reviews;
    }

    /**
     * 6. 数据验证
     */
    private static void validateData(List<OrderRecord> orders, List<PaymentRecord> payments, 
                                     List<ReviewRecord> reviews, Map<String, String> customerMap) {
        System.out.println("  执行数据完整性检查...");
        
        // 验证订单和支付的关联
        Set<String> orderIds = orders.stream().map(o -> o.orderId).collect(Collectors.toSet());
        Set<String> paymentOrderIds = payments.stream().map(p -> p.orderId).collect(Collectors.toSet());
        
        long ordersWithoutPayment = orderIds.stream()
                .filter(id -> !paymentOrderIds.contains(id))
                .count();
        if (ordersWithoutPayment > 0) {
            validationWarnings.add(String.format("有 %d 个订单没有对应的支付记录", ordersWithoutPayment));
        }
        
        long paymentsWithoutOrder = paymentOrderIds.stream()
                .filter(id -> !orderIds.contains(id))
                .count();
        if (paymentsWithoutOrder > 0) {
            validationWarnings.add(String.format("有 %d 个支付记录没有对应的订单", paymentsWithoutOrder));
        }
        
        // 验证订单和评价的关联
        Set<String> reviewOrderIds = reviews.stream().map(r -> r.orderId).collect(Collectors.toSet());
        long ordersWithoutReview = orderIds.stream()
                .filter(id -> !reviewOrderIds.contains(id))
                .count();
        if (ordersWithoutReview > 0) {
            validationWarnings.add(String.format("有 %d 个订单没有对应的评价记录", ordersWithoutReview));
        }
        
        System.out.println("    验证完成");
        System.out.println("    错误数: " + validationErrors.size());
        System.out.println("    警告数: " + validationWarnings.size());
    }

    /**
     * 7. 数据聚合：按客户聚合订单数据
     */
    private static Map<String, CustomerAggregate> aggregateCustomerData(List<OrderRecord> orders) {
        System.out.println("  按客户聚合订单数据...");
        
        Map<String, CustomerAggregate> aggregated = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.customerUniqueId,
                        Collectors.collectingAndThen(Collectors.toList(), orderList -> {
                            CustomerAggregate agg = new CustomerAggregate();
                            agg.customerUniqueId = orderList.get(0).customerUniqueId;
                            agg.customerId = orderList.get(0).customerId;
                            
                            // 聚合统计
                            agg.orderCount = orderList.size();
                            agg.totalAmount = orderList.stream()
                                    .mapToDouble(o -> o.totalAmount)
                                    .sum();
                            agg.lastOrderDate = orderList.stream()
                                    .map(o -> o.purchaseTimestamp)
                                    .filter(Objects::nonNull)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(null);
                            agg.firstOrderDate = orderList.stream()
                                    .map(o -> o.purchaseTimestamp)
                                    .filter(Objects::nonNull)
                                    .min(LocalDateTime::compareTo)
                                    .orElse(null);
                            
                            // 计算平均订单金额
                            agg.averageOrderValue = agg.orderCount > 0 
                                    ? agg.totalAmount / agg.orderCount 
                                    : 0.0;
                            
                            return agg;
                        })
                ));
        
        System.out.println("    聚合客户数: " + aggregated.size());
        
        return aggregated;
    }

    /**
     * 8. 输出处理后的数据
     */
    private static void outputProcessedData(List<OrderRecord> orders, List<PaymentRecord> payments,
                                            List<ReviewRecord> reviews, Map<String, CustomerAggregate> aggregated) throws Exception {
        System.out.println("  输出处理后的数据到 CSV 文件...");
        
        // 输出清洗后的订单数据
        outputOrders(orders);
        
        // 输出清洗后的支付数据
        outputPayments(payments);
        
        // 输出清洗后的评价数据
        outputReviews(reviews);
        
        // 输出聚合后的客户数据
        outputCustomerAggregates(aggregated);
        
        // 输出验证报告
        outputValidationReport();
    }

    private static void outputCustomers(Map<String, CustomerInfo> customerInfoMap) throws Exception {
        File file = new File("processed_customers.csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"customer_id", "customer_unique_id", "customer_zip_code_prefix", 
                    "customer_city", "customer_state"});
            
            for (CustomerInfo info : customerInfoMap.values()) {
                writer.writeNext(new String[]{
                        info.customerId,
                        info.customerUniqueId,
                        info.zipCodePrefix,
                        info.city,
                        info.state
                });
            }
        }
        System.out.println("    已输出: processed_customers.csv");
    }

    private static void outputOrders(List<OrderRecord> orders) throws Exception {
        File file = new File("processed_orders.csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"order_id", "customer_id", "customer_unique_id", "order_status",
                    "order_purchase_timestamp", "order_approved_at", "order_delivered_carrier_date",
                    "order_delivered_customer_date", "order_estimated_delivery_date", "total_amount"});
            
            for (OrderRecord order : orders) {
                writer.writeNext(new String[]{
                        order.orderId,
                        order.customerId,
                        order.customerUniqueId,
                        order.orderStatus,
                        order.purchaseTimestamp != null ? order.purchaseTimestamp.format(DATE_TIME_FORMATTER) : "",
                        order.approvedAt != null ? order.approvedAt.format(DATE_TIME_FORMATTER) : "",
                        order.deliveredCarrierDate != null ? order.deliveredCarrierDate.format(DATE_TIME_FORMATTER) : "",
                        order.deliveredCustomerDate != null ? order.deliveredCustomerDate.format(DATE_TIME_FORMATTER) : "",
                        order.estimatedDeliveryDate != null ? order.estimatedDeliveryDate.format(DATE_TIME_FORMATTER) : "",
                        String.valueOf(order.totalAmount)
                });
            }
        }
        System.out.println("    已输出: processed_orders.csv");
    }

    private static void outputPayments(List<PaymentRecord> payments) throws Exception {
        if (payments.isEmpty()) {
            return;
        }
        
        File file = new File("processed_payments.csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"order_id", "payment_type", "payment_installments", "payment_value"});
            
            for (PaymentRecord payment : payments) {
                writer.writeNext(new String[]{
                        payment.orderId,
                        payment.paymentType,
                        String.valueOf(payment.installments),
                        String.valueOf(payment.paymentValue)
                });
            }
        }
        System.out.println("    已输出: processed_payments.csv");
    }

    private static void outputReviews(List<ReviewRecord> reviews) throws Exception {
        if (reviews.isEmpty()) {
            return;
        }
        
        File file = new File("processed_reviews.csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"review_id", "order_id", "review_score", "review_comment_title",
                    "review_comment_message", "review_creation_date"});
            
            for (ReviewRecord review : reviews) {
                writer.writeNext(new String[]{
                        review.reviewId,
                        review.orderId,
                        String.valueOf(review.reviewScore),
                        review.reviewCommentTitle,
                        review.reviewCommentMessage,
                        review.reviewCreationDate != null ? review.reviewCreationDate.format(DATE_TIME_FORMATTER) : ""
                });
            }
        }
        System.out.println("    已输出: processed_reviews.csv");
    }

    private static void outputCustomerAggregates(Map<String, CustomerAggregate> aggregated) throws Exception {
        File file = new File("processed_customer_aggregates.csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"customer_unique_id", "customer_id", "order_count", "total_amount",
                    "average_order_value", "first_order_date", "last_order_date"});
            
            for (CustomerAggregate agg : aggregated.values()) {
                writer.writeNext(new String[]{
                        agg.customerUniqueId,
                        agg.customerId,
                        String.valueOf(agg.orderCount),
                        String.valueOf(agg.totalAmount),
                        String.valueOf(agg.averageOrderValue),
                        agg.firstOrderDate != null ? agg.firstOrderDate.format(DATE_TIME_FORMATTER) : "",
                        agg.lastOrderDate != null ? agg.lastOrderDate.format(DATE_TIME_FORMATTER) : ""
                });
            }
        }
        System.out.println("    已输出: processed_customer_aggregates.csv");
    }

    private static void outputValidationReport() throws Exception {
        File file = new File("validation_report.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("==========================================");
            writer.println("数据验证报告");
            writer.println("==========================================");
            writer.println();
            
            writer.println("【错误信息】");
            if (validationErrors.isEmpty()) {
                writer.println("  无错误");
            } else {
                for (String error : validationErrors) {
                    writer.println("  - " + error);
                }
            }
            writer.println();
            
            writer.println("【警告信息】");
            if (validationWarnings.isEmpty()) {
                writer.println("  无警告");
            } else {
                for (String warning : validationWarnings) {
                    writer.println("  - " + warning);
                }
            }
        }
        System.out.println("    已输出: validation_report.txt");
    }

    /**
     * 打印统计信息
     */
    private static void printStatistics() {
        System.out.println("\n==========================================");
        System.out.println("数据预处理统计报告");
        System.out.println("==========================================");
        System.out.println("客户数据：");
        System.out.println("  总数: " + totalCustomers);
        System.out.println("  有效: " + validCustomers);
        System.out.println("  无效: " + (totalCustomers - validCustomers));
        System.out.println();
        
        System.out.println("订单数据：");
        System.out.println("  总数: " + totalOrders);
        System.out.println("  有效: " + validOrders);
        System.out.println("  无效: " + invalidOrders);
        System.out.println();
        
        System.out.println("订单明细数据：");
        System.out.println("  总数: " + totalOrderItems);
        System.out.println("  有效: " + validOrderItems);
        System.out.println("  无效: " + (totalOrderItems - validOrderItems));
        System.out.println();
        
        System.out.println("支付数据：");
        System.out.println("  总数: " + totalPayments);
        System.out.println("  有效: " + validPayments);
        System.out.println("  无效: " + (totalPayments - validPayments));
        System.out.println();
        
        System.out.println("数据验证：");
        System.out.println("  错误数: " + validationErrors.size());
        System.out.println("  警告数: " + validationWarnings.size());
    }

    // ==================== 工具方法 ====================

    private static String cleanString(String str) {
        if (str == null) {
            return null;
        }
        str = str.trim();
        return str.isEmpty() ? null : str;
    }

    private static double safeParseDouble(String str, double defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int safeParseInt(String str, int defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static LocalDateTime parseDateTime(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(str.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ==================== 内部类 ====================

    static class CustomerInfo {
        String customerId;
        String customerUniqueId;
        String zipCodePrefix;
        String city;
        String state;
    }

    static class OrderRecord {
        String orderId;
        String customerId;
        String customerUniqueId;
        String orderStatus;
        LocalDateTime purchaseTimestamp;
        LocalDateTime approvedAt;
        LocalDateTime deliveredCarrierDate;
        LocalDateTime deliveredCustomerDate;
        LocalDateTime estimatedDeliveryDate;
        double totalAmount;
    }

    static class PaymentRecord {
        String orderId;
        String paymentType;
        int installments;
        double paymentValue;
    }

    static class ReviewRecord {
        String reviewId;
        String orderId;
        int reviewScore;
        String reviewCommentTitle;
        String reviewCommentMessage;
        LocalDateTime reviewCreationDate;
    }

    static class CustomerAggregate {
        String customerUniqueId;
        String customerId;
        int orderCount;
        double totalAmount;
        double averageOrderValue;
        LocalDateTime firstOrderDate;
        LocalDateTime lastOrderDate;
    }
}

