package com.example.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerce.dto.analysis.*;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderPayment;
import com.example.ecommerce.entity.OrderReview;
import com.example.ecommerce.mapper.OrderItemMapper;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.mapper.OrderPaymentMapper;
import com.example.ecommerce.mapper.OrderReviewMapper;
import com.example.ecommerce.service.SellerAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 卖家维度分析实现：订单数、GMV、好评率 TOPN
 */
@Service
@RequiredArgsConstructor
public class SellerAnalysisServiceImpl implements SellerAnalysisService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderPaymentMapper orderPaymentMapper;
    private final OrderReviewMapper orderReviewMapper;

    @Override
    public List<SellerStatsDTO> getTopSellers(LocalDate startDate, LocalDate endDate, int topN) {
        if (topN <= 0) {
            topN = 10;
        }
        final int finalTopN = topN;

        // 如果没有指定日期范围，使用缓存
        if (startDate == null && endDate == null) {
            List<SellerStatsDTO> cached = cachedTopSellersBySales;
            if (cached != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cached.stream().limit(finalTopN).collect(Collectors.toList());
            }
        }

        // 构建日期条件 SQL
        String dateCondition = "";
        if (startDate != null) {
            dateCondition += " AND o.order_purchase_timestamp >= '" + startDate + " 00:00:00'";
        }
        if (endDate != null) {
            dateCondition += " AND o.order_purchase_timestamp <= '" + endDate.plusDays(1) + " 00:00:00'";
        }

        // 使用 SQL 聚合获取卖家统计数据
        List<Map<String, Object>> sellerStats = orderMapper.aggregateSellerStatsWithDateRange(dateCondition);
        if (CollectionUtils.isEmpty(sellerStats)) {
            return Collections.emptyList();
        }

        // 获取卖家好评率（如果指定了日期范围，需要重新计算）
        List<Map<String, Object>> sellerPositiveRates;
        if (startDate != null || endDate != null) {
            // 有日期范围，需要重新计算好评率
            String positiveRateDateCondition = dateCondition;
            sellerPositiveRates = orderMapper.aggregateSellerPositiveRateWithDateRange(positiveRateDateCondition);
        } else {
            // 无日期范围，使用全量好评率（已缓存）
            sellerPositiveRates = orderMapper.aggregateSellerPositiveRate();
        }

        Map<String, Double> positiveRateMap = new HashMap<>();
        for (Map<String, Object> row : sellerPositiveRates) {
            String sellerId = (String) row.get("sellerId");
            Object positiveCountObj = row.get("positiveCount");
            Object totalReviewedObj = row.get("totalReviewed");
            long positiveCount = positiveCountObj instanceof Number ? ((Number) positiveCountObj).longValue() : 0L;
            long totalReviewed = totalReviewedObj instanceof Number ? ((Number) totalReviewedObj).longValue() : 0L;
            if (totalReviewed > 0) {
                double rate = (double) positiveCount / totalReviewed;
                positiveRateMap.put(sellerId, new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                positiveRateMap.put(sellerId, 0.0);
            }
        }

        // 构建结果
        List<SellerStatsDTO> result = new ArrayList<>();
        for (Map<String, Object> row : sellerStats) {
            SellerStatsDTO dto = new SellerStatsDTO();
            String sellerId = (String) row.get("sellerId");
            dto.setSellerId(sellerId);
            
            Object orderCountObj = row.get("orderCount");
            dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
            
            Object totalGmvObj = row.get("totalGmv");
            if (totalGmvObj instanceof BigDecimal) {
                dto.setTotalGmv((BigDecimal) totalGmvObj);
            } else if (totalGmvObj instanceof Number) {
                dto.setTotalGmv(BigDecimal.valueOf(((Number) totalGmvObj).doubleValue()));
            } else {
                dto.setTotalGmv(BigDecimal.ZERO);
            }

            dto.setPositiveRate(positiveRateMap.getOrDefault(sellerId, 0.0));
            result.add(dto);
        }

        // 排序并限制数量
        return result.stream()
                .sorted(Comparator.comparing(SellerStatsDTO::getTotalGmv).reversed()
                        .thenComparing(SellerStatsDTO::getPositiveRate).reversed())
                .limit(finalTopN)
                .collect(Collectors.toList());
    }

    // 缓存变量
    private volatile List<SellerStatsDTO> cachedTopSellersBySales;
    private volatile List<SellerSalesDistributionDTO> cachedSellerSalesDistribution;
    private volatile List<SellerRepurchaseRateDTO> cachedTopSellersRepurchaseRate;
    private volatile List<SellerAverageOrderValueDTO> cachedTopSellersAverageOrderValue;
    private volatile List<SellerOrderTrendDTO> cachedSellerOrderTrend;
    private volatile List<SellerReviewDistributionDTO> cachedSellerReviewDistribution;
    private volatile long cachedAtMillis;
    private static final long CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object calculationLock = new Object();

    @Override
    public List<SellerStatsDTO> getTopSellersBySales(int topN) {
        if (topN <= 0) {
            topN = 20;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedTopSellersBySales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedTopSellersBySales.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedTopSellersBySales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedTopSellersBySales.stream().limit(finalTopN).collect(Collectors.toList());
            }

            // 计算所有卖家销售额（不限制数量，用于缓存）
            List<SellerStatsDTO> allSellers = calculateAllSellersStats();
            cachedTopSellersBySales = allSellers;
            cachedAtMillis = System.currentTimeMillis();

            return allSellers.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<SellerSalesDistributionDTO> getSellerSalesDistribution() {
        // 检查缓存
        if (cachedSellerSalesDistribution != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedSellerSalesDistribution;
        }

        synchronized (calculationLock) {
            if (cachedSellerSalesDistribution != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedSellerSalesDistribution;
            }

            List<SellerStatsDTO> allSellers = calculateAllSellersStats();
            Map<String, Long> bucketCounts = new LinkedHashMap<>();
            bucketCounts.put("0-1000", 0L);
            bucketCounts.put("1000-5000", 0L);
            bucketCounts.put("5000-10000", 0L);
            bucketCounts.put("10000-50000", 0L);
            bucketCounts.put("50000-100000", 0L);
            bucketCounts.put("100000+", 0L);

            for (SellerStatsDTO seller : allSellers) {
                double sales = seller.getTotalGmv().doubleValue();
                String bucket;
                if (sales < 1000) {
                    bucket = "0-1000";
                } else if (sales < 5000) {
                    bucket = "1000-5000";
                } else if (sales < 10000) {
                    bucket = "5000-10000";
                } else if (sales < 50000) {
                    bucket = "10000-50000";
                } else if (sales < 100000) {
                    bucket = "50000-100000";
                } else {
                    bucket = "100000+";
                }
                bucketCounts.put(bucket, bucketCounts.get(bucket) + 1);
            }

            List<SellerSalesDistributionDTO> result = bucketCounts.entrySet().stream()
                    .map(entry -> {
                        SellerSalesDistributionDTO dto = new SellerSalesDistributionDTO();
                        dto.setSalesRange(entry.getKey());
                        dto.setSellerCount(entry.getValue());
                        return dto;
                    })
                    .collect(Collectors.toList());

            cachedSellerSalesDistribution = result;
            return result;
        }
    }

    @Override
    public List<SellerRepurchaseRateDTO> getTopSellersRepurchaseRate(int topN) {
        if (topN <= 0) {
            topN = 20;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedTopSellersRepurchaseRate != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedTopSellersRepurchaseRate.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedTopSellersRepurchaseRate != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedTopSellersRepurchaseRate.stream().limit(finalTopN).collect(Collectors.toList());
            }

            // 使用 SQL 聚合优化：直接在数据库层面统计卖家-客户订单数
            List<Map<String, Object>> sellerCustomerData = orderMapper.aggregateSellerCustomerOrderCount();
            
            // 按卖家分组统计（使用 customer_unique_id）
            Map<String, Map<String, Integer>> sellerCustomerOrderCount = new HashMap<>();
            for (Map<String, Object> row : sellerCustomerData) {
                String sellerId = (String) row.get("sellerId");
                // SQL 查询现在返回 customerUniqueId
                String customerUniqueId = (String) row.get("customerUniqueId");
                if (customerUniqueId == null) {
                    // 兼容旧数据格式
                    customerUniqueId = (String) row.get("customerId");
                }
                Object orderCountObj = row.get("orderCount");
                int orderCount = orderCountObj instanceof Number ? ((Number) orderCountObj).intValue() : 0;
                
                sellerCustomerOrderCount.computeIfAbsent(sellerId, k -> new HashMap<>())
                        .put(customerUniqueId, orderCount);
            }

            List<SellerRepurchaseRateDTO> result = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> entry : sellerCustomerOrderCount.entrySet()) {
                String sellerId = entry.getKey();
                Map<String, Integer> customerOrders = entry.getValue();
                long totalCustomers = customerOrders.size();
                long repurchaseCustomers = customerOrders.values().stream().mapToLong(count -> count > 1 ? 1 : 0).sum();
                double repurchaseRate = totalCustomers > 0 ? (double) repurchaseCustomers / totalCustomers : 0.0;

                SellerRepurchaseRateDTO dto = new SellerRepurchaseRateDTO();
                dto.setSellerId(sellerId);
                dto.setRepurchaseRate(repurchaseRate);
                dto.setTotalCustomers(totalCustomers);
                dto.setRepurchaseCustomers(repurchaseCustomers);
                result.add(dto);
            }

            result.sort(Comparator.comparing(SellerRepurchaseRateDTO::getRepurchaseRate).reversed());
            cachedTopSellersRepurchaseRate = result;
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<SellerAverageOrderValueDTO> getTopSellersAverageOrderValue(int topN) {
        if (topN <= 0) {
            topN = 20;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedTopSellersAverageOrderValue != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedTopSellersAverageOrderValue.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedTopSellersAverageOrderValue != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedTopSellersAverageOrderValue.stream().limit(finalTopN).collect(Collectors.toList());
            }

            List<SellerStatsDTO> allSellers = calculateAllSellersStats();

            List<SellerAverageOrderValueDTO> result = new ArrayList<>();
            for (SellerStatsDTO seller : allSellers) {
                String sellerId = seller.getSellerId();
                long orderCount = seller.getOrderCount();
                BigDecimal totalGmv = seller.getTotalGmv();
                BigDecimal avgOrderValue = orderCount > 0
                        ? totalGmv.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                SellerAverageOrderValueDTO dto = new SellerAverageOrderValueDTO();
                dto.setSellerId(sellerId);
                dto.setAverageOrderValue(avgOrderValue);
                dto.setOrderCount(orderCount);
                dto.setTotalGmv(totalGmv);
                result.add(dto);
            }

            result.sort(Comparator.comparing(SellerAverageOrderValueDTO::getAverageOrderValue).reversed());
            cachedTopSellersAverageOrderValue = result;
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<SellerOrderTrendDTO> getSellerOrderTrend(int topN) {
        if (topN <= 0) {
            topN = 20;
        }

        // 检查缓存
        if (cachedSellerOrderTrend != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedSellerOrderTrend;
        }

        synchronized (calculationLock) {
            if (cachedSellerOrderTrend != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedSellerOrderTrend;
            }

            // 获取 Top N 卖家
            List<SellerStatsDTO> topSellers = getTopSellersBySales(topN);
            Set<String> topSellerIds = topSellers.stream().map(SellerStatsDTO::getSellerId).collect(Collectors.toSet());

            // 使用 SQL 聚合优化：直接在数据库层面统计卖家订单量趋势
            List<Map<String, Object>> sellerTrendData = orderMapper.aggregateSellerOrderTrend();
            
            List<SellerOrderTrendDTO> result = new ArrayList<>();
            for (Map<String, Object> row : sellerTrendData) {
                String sellerId = (String) row.get("sellerId");
                if (!topSellerIds.contains(sellerId)) continue;
                
                String month = (String) row.get("month");
                Object orderCountObj = row.get("orderCount");
                long orderCount = orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L;
                
                SellerOrderTrendDTO dto = new SellerOrderTrendDTO();
                dto.setSellerId(sellerId);
                dto.setMonth(month);
                dto.setOrderCount(orderCount);
                result.add(dto);
            }

            cachedSellerOrderTrend = result;
            return result;
        }
    }

    @Override
    public List<SellerReviewDistributionDTO> getSellerReviewDistribution(int topN) {
        if (topN <= 0) {
            topN = 20;
        }

        // 检查缓存
        if (cachedSellerReviewDistribution != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedSellerReviewDistribution;
        }

        synchronized (calculationLock) {
            if (cachedSellerReviewDistribution != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedSellerReviewDistribution;
            }

            // 获取 Top N 卖家
            List<SellerStatsDTO> topSellers = getTopSellersBySales(topN);
            List<String> topSellerIds = topSellers.stream().map(SellerStatsDTO::getSellerId).collect(Collectors.toList());
            
            if (topSellerIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 构建 sellerIds 字符串用于 SQL IN 查询
            // 注意：由于 MyBatis 注解不支持动态 IN，这里使用字符串拼接，但只查询 Top N（通常20个），风险可控
            String sellerIdsStr = topSellerIds.stream()
                    .map(id -> "'" + id.replace("'", "''") + "'") // SQL 注入防护：转义单引号
                    .collect(Collectors.joining(","));
            
            // 使用 SQL 聚合查询 Top N 卖家的评分
            List<Map<String, Object>> reviewScoresData = orderMapper.aggregateSellerReviewScores(sellerIdsStr);

            // 按卖家收集评分
            Map<String, List<Integer>> sellerScores = new HashMap<>();
            Set<String> topSellerIdsSet = new HashSet<>(topSellerIds);
            for (Map<String, Object> row : reviewScoresData) {
                String sellerId = (String) row.get("sellerId");
                if (!topSellerIdsSet.contains(sellerId)) continue;
                Object scoreObj = row.get("reviewScore");
                if (scoreObj == null) continue;
                int score = scoreObj instanceof Number ? ((Number) scoreObj).intValue() : 0;
                if (score > 0 && score <= 5) {
                    sellerScores.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(score);
                }
            }

            List<SellerReviewDistributionDTO> result = new ArrayList<>();
            for (Map.Entry<String, List<Integer>> entry : sellerScores.entrySet()) {
                String sellerId = entry.getKey();
                List<Integer> scores = entry.getValue();
                if (scores.isEmpty()) continue;

                scores.sort(Integer::compareTo);
                double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                int min = scores.get(0);
                int max = scores.get(scores.size() - 1);
                int median = scores.get(scores.size() / 2);

                SellerReviewDistributionDTO dto = new SellerReviewDistributionDTO();
                dto.setSellerId(sellerId);
                dto.setReviewScores(scores);
                dto.setAverageScore(avg);
                dto.setMinScore(min);
                dto.setMaxScore(max);
                dto.setMedianScore(median);
                result.add(dto);
            }

            cachedSellerReviewDistribution = result;
            return result;
        }
    }

    /**
     * 计算所有卖家统计数据（用于缓存）- 使用 SQL 聚合优化
     */
    private List<SellerStatsDTO> calculateAllSellersStats() {
        // 使用 SQL 聚合获取卖家基本统计（订单数、GMV）
        List<Map<String, Object>> sellerStats = orderMapper.aggregateSellerStats();
        if (CollectionUtils.isEmpty(sellerStats)) {
            return Collections.emptyList();
        }

        // 使用 SQL 聚合获取卖家好评率
        List<Map<String, Object>> sellerPositiveRates = orderMapper.aggregateSellerPositiveRate();
        Map<String, Double> positiveRateMap = new HashMap<>();
        for (Map<String, Object> row : sellerPositiveRates) {
            String sellerId = (String) row.get("sellerId");
            Object positiveCountObj = row.get("positiveCount");
            Object totalReviewedObj = row.get("totalReviewed");
            long positiveCount = positiveCountObj instanceof Number ? ((Number) positiveCountObj).longValue() : 0L;
            long totalReviewed = totalReviewedObj instanceof Number ? ((Number) totalReviewedObj).longValue() : 0L;
            if (totalReviewed > 0) {
                double rate = (double) positiveCount / totalReviewed;
                positiveRateMap.put(sellerId, new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                positiveRateMap.put(sellerId, 0.0);
            }
        }

        // 合并数据
        List<SellerStatsDTO> result = new ArrayList<>();
        for (Map<String, Object> row : sellerStats) {
            SellerStatsDTO dto = new SellerStatsDTO();
            String sellerId = (String) row.get("sellerId");
            dto.setSellerId(sellerId);
            
            Object orderCountObj = row.get("orderCount");
            dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
            
            Object totalGmvObj = row.get("totalGmv");
            if (totalGmvObj instanceof BigDecimal) {
                dto.setTotalGmv((BigDecimal) totalGmvObj);
            } else if (totalGmvObj instanceof Number) {
                dto.setTotalGmv(BigDecimal.valueOf(((Number) totalGmvObj).doubleValue()));
            } else {
                dto.setTotalGmv(BigDecimal.ZERO);
            }

            // 从 Map 中获取好评率
            dto.setPositiveRate(positiveRateMap.getOrDefault(sellerId, 0.0));

            result.add(dto);
        }

        return result;
    }
}


