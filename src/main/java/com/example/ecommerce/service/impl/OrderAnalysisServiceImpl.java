package com.example.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerce.dto.analysis.AverageOrderValueDTO;
import com.example.ecommerce.dto.analysis.CustomerAmountDistributionDTO;
import com.example.ecommerce.dto.analysis.ForecastPointDTO;
import com.example.ecommerce.dto.analysis.KpiOverviewDTO;
import com.example.ecommerce.dto.analysis.TimeSeriesPointDTO;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderPayment;
import com.example.ecommerce.mapper.CustomerMapper;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.mapper.OrderPaymentMapper;
import com.example.ecommerce.service.OrderAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * 订单 / 运营指标分析实现
 */
@Service
@RequiredArgsConstructor
public class OrderAnalysisServiceImpl implements OrderAnalysisService {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final OrderPaymentMapper orderPaymentMapper;

    // 运营指标概览缓存
    private volatile KpiOverviewDTO cachedKpiOverview;
    private volatile long cachedKpiOverviewAtMillis;
    private static final long KPI_OVERVIEW_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object kpiOverviewCalculationLock = new Object();

    @Override
    public KpiOverviewDTO getKpiOverview(LocalDate startDate, LocalDate endDate) {
        // 如果没有指定日期范围，使用缓存
        if (startDate == null && endDate == null) {
            KpiOverviewDTO snapshot = cachedKpiOverview;
            if (snapshot != null && (System.currentTimeMillis() - cachedKpiOverviewAtMillis) < KPI_OVERVIEW_CACHE_DURATION_MILLIS) {
                return snapshot;
            }

            synchronized (kpiOverviewCalculationLock) {
                snapshot = cachedKpiOverview;
                if (snapshot != null && (System.currentTimeMillis() - cachedKpiOverviewAtMillis) < KPI_OVERVIEW_CACHE_DURATION_MILLIS) {
                    return snapshot;
                }
                KpiOverviewDTO result = calculateKpiOverview(startDate, endDate);
                cachedKpiOverview = result;
                cachedKpiOverviewAtMillis = System.currentTimeMillis();
                return result;
            }
        }

        // 如果指定了日期范围，直接计算（不缓存）
        return calculateKpiOverview(startDate, endDate);
    }

    private KpiOverviewDTO calculateKpiOverview(LocalDate startDate, LocalDate endDate) {
        // 如果指定了日期范围，使用旧的方式（暂时不支持日期范围过滤的 SQL 聚合）
        if (startDate != null || endDate != null) {
            return calculateKpiOverviewLegacy(startDate, endDate);
        }
        
        // 使用 SQL 聚合优化
        Map<String, Object> result = orderMapper.aggregateKpiOverview();
        if (result == null || result.isEmpty()) {
            return new KpiOverviewDTO();
        }
        
        KpiOverviewDTO dto = new KpiOverviewDTO();
        dto.setCustomerCount(((Number) result.get("customerCount")).longValue());
        dto.setOrderCount(((Number) result.get("orderCount")).longValue());
        dto.setValidOrderCount(((Number) result.get("validOrderCount")).longValue());
        
        Object totalGmvObj = result.get("totalGmv");
        BigDecimal totalGmv = totalGmvObj instanceof BigDecimal 
                ? (BigDecimal) totalGmvObj 
                : BigDecimal.valueOf(((Number) totalGmvObj).doubleValue());
        dto.setTotalGmv(totalGmv);
        
        long orderCount = dto.getOrderCount();
        if (orderCount > 0 && totalGmv != null) {
            dto.setAverageOrderValue(
                    totalGmv.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
            );
        } else {
            dto.setAverageOrderValue(BigDecimal.ZERO);
        }
        
        return dto;
    }
    
    private KpiOverviewDTO calculateKpiOverviewLegacy(LocalDate startDate, LocalDate endDate) {
        KpiOverviewDTO dto = new KpiOverviewDTO();

        long customerCount = customerMapper.selectCount(null);
        dto.setCustomerCount(customerCount);

        QueryWrapper<Order> orderWrapper = buildOrderDateRangeWrapper(startDate, endDate);
        long orderCount = orderMapper.selectCount(orderWrapper);
        dto.setOrderCount(orderCount);

        // 简单认为 delivered 的订单为有效订单，可根据业务需要调整
        QueryWrapper<Order> validWrapper = buildOrderDateRangeWrapper(startDate, endDate)
                .eq("order_status", "delivered");
        long validOrderCount = orderMapper.selectCount(validWrapper);
        dto.setValidOrderCount(validOrderCount);

        // 统计 GMV（从订单支付表中按时间范围聚合）
        QueryWrapper<OrderPayment> paymentWrapper = new QueryWrapper<>();
        if (startDate != null) {
            paymentWrapper.ge("create_time", startDate.atStartOfDay());
        }
        if (endDate != null) {
            paymentWrapper.le("create_time", endDate.plusDays(1).atStartOfDay());
        }
        List<OrderPayment> payments = orderPaymentMapper.selectList(paymentWrapper);
        BigDecimal totalGmv = payments.stream()
                .map(p -> Optional.ofNullable(p.getPaymentValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalGmv(totalGmv);

        if (orderCount > 0 && totalGmv != null) {
            dto.setAverageOrderValue(
                    totalGmv.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
            );
        } else {
            dto.setAverageOrderValue(BigDecimal.ZERO);
        }

        return dto;
    }

    // 订单趋势缓存
    private volatile List<TimeSeriesPointDTO> cachedOrderTrend;
    private volatile long cachedOrderTrendAtMillis;
    private static final long ORDER_TREND_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object orderTrendCalculationLock = new Object();

    @Override
    public List<TimeSeriesPointDTO> getOrderTrend(LocalDate startDate, LocalDate endDate) {
        // 如果没有指定日期范围，使用缓存
        if (startDate == null && endDate == null) {
            List<TimeSeriesPointDTO> snapshot = cachedOrderTrend;
            if (snapshot != null && (System.currentTimeMillis() - cachedOrderTrendAtMillis) < ORDER_TREND_CACHE_DURATION_MILLIS) {
                return snapshot;
            }

            synchronized (orderTrendCalculationLock) {
                snapshot = cachedOrderTrend;
                if (snapshot != null && (System.currentTimeMillis() - cachedOrderTrendAtMillis) < ORDER_TREND_CACHE_DURATION_MILLIS) {
                    return snapshot;
                }
                List<TimeSeriesPointDTO> result = calculateOrderTrend(startDate, endDate);
                cachedOrderTrend = result;
                cachedOrderTrendAtMillis = System.currentTimeMillis();
                return result;
            }
        }

        // 如果指定了日期范围，直接计算（不缓存）
        return calculateOrderTrend(startDate, endDate);
    }

    private List<TimeSeriesPointDTO> calculateOrderTrend(LocalDate startDate, LocalDate endDate) {
        // 如果指定了日期范围，使用旧的方式（暂时不支持日期范围过滤的 SQL 聚合）
        if (startDate != null || endDate != null) {
            return calculateOrderTrendLegacy(startDate, endDate);
        }
        
        // 使用 SQL 聚合优化
        List<Map<String, Object>> aggregatedData = orderMapper.aggregateOrderTrend();
        if (CollectionUtils.isEmpty(aggregatedData)) {
            return Collections.emptyList();
        }
        
        Map<LocalDate, Long> countMap = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> gmvMap = new LinkedHashMap<>();
        
        for (Map<String, Object> row : aggregatedData) {
            Object dateObj = row.get("date");
            LocalDate date = null;
            if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof LocalDate) {
                date = (LocalDate) dateObj;
            } else if (dateObj instanceof java.util.Date) {
                date = ((java.util.Date) dateObj).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            }
            
            if (date != null) {
                Object orderCountObj = row.get("orderCount");
                long orderCount = orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L;
                countMap.put(date, orderCount);
                
                Object gmvObj = row.get("gmv");
                BigDecimal gmv = gmvObj instanceof BigDecimal 
                        ? (BigDecimal) gmvObj 
                        : BigDecimal.valueOf(((Number) gmvObj).doubleValue());
                gmvMap.put(date, gmv);
            }
        }
        
        return buildTimeSeries(countMap, gmvMap);
    }
    
    private List<TimeSeriesPointDTO> calculateOrderTrendLegacy(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Order> wrapper = buildOrderDateRangeWrapper(startDate, endDate);
        List<Order> orders = orderMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        // 先按日期聚合订单数
        Map<LocalDate, Long> countMap = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getOrderPurchaseTimestamp().toLocalDate(),
                        Collectors.counting()
                ));

        // 再从支付表统计 GMV（按订单维度聚合）
        Set<String> orderIds = orders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return buildTimeSeries(countMap, Collections.emptyMap());
        }

        QueryWrapper<OrderPayment> paymentWrapper = new QueryWrapper<>();
        paymentWrapper.in("order_id", orderIds);
        List<OrderPayment> payments = orderPaymentMapper.selectList(paymentWrapper);
        Map<String, BigDecimal> orderGmvMap = payments.stream()
                .collect(Collectors.groupingBy(
                        OrderPayment::getOrderId,
                        Collectors.mapping(
                                p -> Optional.ofNullable(p.getPaymentValue()).orElse(BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        Map<LocalDate, BigDecimal> dateGmvMap = new HashMap<>();
        for (Order order : orders) {
            LocalDate date = order.getOrderPurchaseTimestamp().toLocalDate();
            BigDecimal orderGmv = orderGmvMap.getOrDefault(order.getOrderId(), BigDecimal.ZERO);
            dateGmvMap.merge(date, orderGmv, BigDecimal::add);
        }

        return buildTimeSeries(countMap, dateGmvMap);
    }

    @Override
    public List<ForecastPointDTO> forecastOrderVolume(int days) {
        if (days <= 0) {
            days = 7;
        }
        // 使用历史订单按日聚合，计算简单的移动平均作为预测值
        List<TimeSeriesPointDTO> history = getOrderTrend(null, LocalDate.now());
        if (CollectionUtils.isEmpty(history)) {
            return Collections.emptyList();
        }
        // 取最近 N 天订单数做平均
        int window = Math.min(14, history.size());
        List<TimeSeriesPointDTO> recent = history.subList(history.size() - window, history.size());
        double avg = recent.stream().mapToLong(TimeSeriesPointDTO::getOrderCount).average().orElse(0);

        List<ForecastPointDTO> result = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        for (int i = 0; i < days; i++) {
            ForecastPointDTO dto = new ForecastPointDTO();
            dto.setDate(start.plusDays(i));
            dto.setPredictedOrderCount(Math.round(avg));
            result.add(dto);
        }
        return result;
    }

    private QueryWrapper<Order> buildOrderDateRangeWrapper(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if (startDate != null) {
            wrapper.ge("order_purchase_timestamp", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le("order_purchase_timestamp", endDate.plusDays(1).atStartOfDay());
        }
        return wrapper;
    }

    private List<TimeSeriesPointDTO> buildTimeSeries(Map<LocalDate, Long> countMap,
                                                     Map<LocalDate, BigDecimal> gmvMap) {
        Set<LocalDate> allDates = new HashSet<>(countMap.keySet());
        allDates.addAll(gmvMap.keySet());
        List<LocalDate> sortedDates = allDates.stream()
                .sorted()
                .collect(Collectors.toList());

        List<TimeSeriesPointDTO> list = new ArrayList<>();
        for (LocalDate date : sortedDates) {
            TimeSeriesPointDTO dto = new TimeSeriesPointDTO();
            dto.setDate(date);
            dto.setOrderCount(countMap.getOrDefault(date, 0L));
            dto.setGmv(gmvMap.getOrDefault(date, BigDecimal.ZERO));
            list.add(dto);
        }
        return list;
    }

    // 订单金额趋势缓存
    private volatile List<TimeSeriesPointDTO> cachedOrderAmountTrend;
    private volatile String cachedOrderAmountTrendGroupBy;
    private volatile long cachedOrderAmountTrendAtMillis;
    private static final long ORDER_AMOUNT_TREND_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object orderAmountTrendCalculationLock = new Object();

    @Override
    public List<TimeSeriesPointDTO> getOrderAmountTrend(LocalDate startDate, LocalDate endDate, String groupBy) {
        // 默认按日聚合
        if (groupBy == null || groupBy.isEmpty()) {
            groupBy = "day";
        }

        // 检查缓存
        if (cachedOrderAmountTrend != null 
                && groupBy.equals(cachedOrderAmountTrendGroupBy)
                && (System.currentTimeMillis() - cachedOrderAmountTrendAtMillis) < ORDER_AMOUNT_TREND_CACHE_DURATION_MILLIS) {
            return cachedOrderAmountTrend;
        }

        synchronized (orderAmountTrendCalculationLock) {
            if (cachedOrderAmountTrend != null 
                    && groupBy.equals(cachedOrderAmountTrendGroupBy)
                    && (System.currentTimeMillis() - cachedOrderAmountTrendAtMillis) < ORDER_AMOUNT_TREND_CACHE_DURATION_MILLIS) {
                return cachedOrderAmountTrend;
            }

            List<TimeSeriesPointDTO> dailyData = getOrderTrend(startDate, endDate);
            if (CollectionUtils.isEmpty(dailyData)) {
                return Collections.emptyList();
            }

            List<TimeSeriesPointDTO> result;
            if ("day".equalsIgnoreCase(groupBy)) {
                result = dailyData;
            } else {
                // 按周或月聚合
                Map<String, TimeSeriesPointDTO> groupedMap = new LinkedHashMap<>();
                for (TimeSeriesPointDTO point : dailyData) {
                    String key;
                    if ("week".equalsIgnoreCase(groupBy)) {
                        // 使用年份和周数作为key
                        int year = point.getDate().getYear();
                        int week = point.getDate().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                        key = String.format("%d-W%02d", year, week);
                    } else if ("month".equalsIgnoreCase(groupBy)) {
                        // 使用年月作为key
                        key = point.getDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                    } else {
                        key = point.getDate().toString();
                    }

                    groupedMap.compute(key, (k, existing) -> {
                        if (existing == null) {
                            TimeSeriesPointDTO dto = new TimeSeriesPointDTO();
                            dto.setDate(point.getDate()); // 使用该组的第一天作为日期
                            dto.setOrderCount(point.getOrderCount());
                            dto.setGmv(point.getGmv());
                            return dto;
                        } else {
                            existing.setOrderCount(existing.getOrderCount() + point.getOrderCount());
                            existing.setGmv(existing.getGmv().add(point.getGmv()));
                            return existing;
                        }
                    });
                }
                result = new ArrayList<>(groupedMap.values());
            }

            // 更新缓存
            cachedOrderAmountTrend = result;
            cachedOrderAmountTrendGroupBy = groupBy;
            cachedOrderAmountTrendAtMillis = System.currentTimeMillis();

            return result;
        }
    }

    // 客单价趋势缓存
    private volatile List<AverageOrderValueDTO> cachedAverageOrderValueTrend;
    private volatile long cachedAverageOrderValueTrendAtMillis;
    private static final long AVERAGE_ORDER_VALUE_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object averageOrderValueCalculationLock = new Object();

    @Override
    public List<AverageOrderValueDTO> getAverageOrderValueTrend(LocalDate startDate, LocalDate endDate) {
        // 如果指定了日期范围，使用旧的方式（暂时不支持日期范围过滤的 SQL 聚合）
        if (startDate != null || endDate != null) {
            return calculateAverageOrderValueTrendLegacy(startDate, endDate);
        }
        
        // 检查缓存
        List<AverageOrderValueDTO> snapshot = cachedAverageOrderValueTrend;
        if (snapshot != null && (System.currentTimeMillis() - cachedAverageOrderValueTrendAtMillis) < AVERAGE_ORDER_VALUE_CACHE_DURATION_MILLIS) {
            return snapshot;
        }

        synchronized (averageOrderValueCalculationLock) {
            snapshot = cachedAverageOrderValueTrend;
            if (snapshot != null && (System.currentTimeMillis() - cachedAverageOrderValueTrendAtMillis) < AVERAGE_ORDER_VALUE_CACHE_DURATION_MILLIS) {
                return snapshot;
            }

            // 使用 SQL 聚合优化
            List<Map<String, Object>> aggregatedData = orderMapper.aggregateAverageOrderValueTrend();
            if (CollectionUtils.isEmpty(aggregatedData)) {
                return Collections.emptyList();
            }

            List<AverageOrderValueDTO> result = new ArrayList<>();
            for (Map<String, Object> row : aggregatedData) {
                AverageOrderValueDTO dto = new AverageOrderValueDTO();
                dto.setMonth((String) row.get("month"));
                
                Object orderCountObj = row.get("orderCount");
                long orderCount = orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L;
                dto.setOrderCount(orderCount);
                
                Object totalGmvObj = row.get("totalGmv");
                BigDecimal totalGmv = totalGmvObj instanceof BigDecimal 
                        ? (BigDecimal) totalGmvObj 
                        : BigDecimal.valueOf(((Number) totalGmvObj).doubleValue());
                dto.setTotalGmv(totalGmv);
                
                Object avgOrderValueObj = row.get("averageOrderValue");
                BigDecimal avgOrderValue = avgOrderValueObj instanceof BigDecimal 
                        ? (BigDecimal) avgOrderValueObj 
                        : BigDecimal.valueOf(((Number) avgOrderValueObj).doubleValue());
                dto.setAverageOrderValue(avgOrderValue);
                
                result.add(dto);
            }

            // 更新缓存
            cachedAverageOrderValueTrend = result;
            cachedAverageOrderValueTrendAtMillis = System.currentTimeMillis();

            return result;
        }
    }
    
    private List<AverageOrderValueDTO> calculateAverageOrderValueTrendLegacy(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Order> wrapper = buildOrderDateRangeWrapper(startDate, endDate);
        List<Order> orders = orderMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        // 按月份聚合订单
        Map<String, List<Order>> monthOrderMap = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getOrderPurchaseTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                ));

        // 获取订单ID集合
        Set<String> orderIds = orders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toSet());

        // 查询支付数据
        QueryWrapper<OrderPayment> paymentWrapper = new QueryWrapper<>();
        paymentWrapper.in("order_id", orderIds);
        List<OrderPayment> payments = orderPaymentMapper.selectList(paymentWrapper);
        Map<String, BigDecimal> orderGmvMap = payments.stream()
                .collect(Collectors.groupingBy(
                        OrderPayment::getOrderId,
                        Collectors.mapping(
                                p -> Optional.ofNullable(p.getPaymentValue()).orElse(BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // 按月份统计GMV和订单数
        Map<String, BigDecimal> monthGmvMap = new HashMap<>();
        Map<String, Long> monthOrderCountMap = new HashMap<>();

        for (Order order : orders) {
            String month = order.getOrderPurchaseTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            BigDecimal orderGmv = orderGmvMap.getOrDefault(order.getOrderId(), BigDecimal.ZERO);
            monthGmvMap.merge(month, orderGmv, BigDecimal::add);
            monthOrderCountMap.merge(month, 1L, Long::sum);
        }

        // 构建结果
        List<AverageOrderValueDTO> result = new ArrayList<>();
        for (String month : monthGmvMap.keySet().stream().sorted().collect(Collectors.toList())) {
            AverageOrderValueDTO dto = new AverageOrderValueDTO();
            dto.setMonth(month);
            dto.setTotalGmv(monthGmvMap.get(month));
            dto.setOrderCount(monthOrderCountMap.getOrDefault(month, 0L));

            long orderCount = dto.getOrderCount();
            if (orderCount > 0) {
                dto.setAverageOrderValue(
                        dto.getTotalGmv().divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                );
            } else {
                dto.setAverageOrderValue(BigDecimal.ZERO);
            }
            result.add(dto);
        }

        return result;
    }

    // 消费金额分布缓存
    private volatile List<CustomerAmountDistributionDTO> cachedCustomerAmountDistribution;
    private volatile long cachedCustomerAmountDistributionAtMillis;
    private static final long CUSTOMER_AMOUNT_DIST_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object customerAmountDistCalculationLock = new Object();

    @Override
    public List<CustomerAmountDistributionDTO> getCustomerAmountDistribution() {
        // 检查缓存
        List<CustomerAmountDistributionDTO> snapshot = cachedCustomerAmountDistribution;
        if (snapshot != null && (System.currentTimeMillis() - cachedCustomerAmountDistributionAtMillis) < CUSTOMER_AMOUNT_DIST_CACHE_DURATION_MILLIS) {
            return snapshot;
        }

        synchronized (customerAmountDistCalculationLock) {
            snapshot = cachedCustomerAmountDistribution;
            if (snapshot != null && (System.currentTimeMillis() - cachedCustomerAmountDistributionAtMillis) < CUSTOMER_AMOUNT_DIST_CACHE_DURATION_MILLIS) {
                return snapshot;
            }

            // 使用 SQL 聚合优化：直接在数据库层面聚合客户总金额
            List<Map<String, Object>> customerAmounts = orderMapper.aggregateCustomerTotalAmount();
            if (CollectionUtils.isEmpty(customerAmounts)) {
                return Collections.emptyList();
            }

            // 定义金额区间：0-50, 50-100, 100-200, 200-500, 500-1000, 1000-2000, 2000+
            Map<String, Long> bucketCounts = new LinkedHashMap<>();
            bucketCounts.put("0-50", 0L);
            bucketCounts.put("50-100", 0L);
            bucketCounts.put("100-200", 0L);
            bucketCounts.put("200-500", 0L);
            bucketCounts.put("500-1000", 0L);
            bucketCounts.put("1000-2000", 0L);
            bucketCounts.put("2000+", 0L);

            // 分桶统计
            for (Map<String, Object> row : customerAmounts) {
                Object totalAmountObj = row.get("totalAmount");
                BigDecimal totalAmount;
                if (totalAmountObj instanceof BigDecimal) {
                    totalAmount = (BigDecimal) totalAmountObj;
                } else if (totalAmountObj instanceof Number) {
                    totalAmount = BigDecimal.valueOf(((Number) totalAmountObj).doubleValue());
                } else {
                    totalAmount = BigDecimal.ZERO;
                }

                String bucket;
                if (totalAmount.compareTo(BigDecimal.valueOf(50)) < 0) {
                    bucket = "0-50";
                } else if (totalAmount.compareTo(BigDecimal.valueOf(100)) < 0) {
                    bucket = "50-100";
                } else if (totalAmount.compareTo(BigDecimal.valueOf(200)) < 0) {
                    bucket = "100-200";
                } else if (totalAmount.compareTo(BigDecimal.valueOf(500)) < 0) {
                    bucket = "200-500";
                } else if (totalAmount.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    bucket = "500-1000";
                } else if (totalAmount.compareTo(BigDecimal.valueOf(2000)) < 0) {
                    bucket = "1000-2000";
                } else {
                    bucket = "2000+";
                }
                bucketCounts.put(bucket, bucketCounts.get(bucket) + 1);
            }

            List<CustomerAmountDistributionDTO> result = bucketCounts.entrySet().stream()
                    .map(entry -> {
                        CustomerAmountDistributionDTO dto = new CustomerAmountDistributionDTO();
                        dto.setAmountRange(entry.getKey());
                        dto.setCustomerCount(entry.getValue());
                        return dto;
                    })
                    .collect(Collectors.toList());

            // 更新缓存
            cachedCustomerAmountDistribution = result;
            cachedCustomerAmountDistributionAtMillis = System.currentTimeMillis();

            return result;
        }
    }
}


