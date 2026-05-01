package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.analysis.*;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.service.GeoAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 地理维度分析实现：基于 customers + geolocation + orders
 * 使用 SQL 聚合优化，避免加载全量数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoAnalysisServiceImpl implements GeoAnalysisService {

    private final OrderMapper orderMapper;

    // 缓存变量
    private volatile List<StateSalesHeatmapDTO> cachedStateSalesHeatmap;
    private volatile List<StateOrderCountDTO> cachedStateOrderCount;
    private volatile List<StateAverageOrderValueDTO> cachedStateAverageOrderValue;
    private volatile List<StateRepurchaseRateDTO> cachedStateRepurchaseRate;
    private volatile List<LogisticsEfficiencyDTO> cachedLogisticsEfficiency;
    private volatile List<GeoUserGrowthDTO> cachedGeoUserGrowth;
    private volatile long cachedAtMillis;
    private static final long CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object calculationLock = new Object();

    @Override
    public List<StateSalesHeatmapDTO> getStateSalesHeatmap() {
        // 检查缓存
        if (cachedStateSalesHeatmap != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedStateSalesHeatmap;
        }

        synchronized (calculationLock) {
            if (cachedStateSalesHeatmap != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedStateSalesHeatmap;
            }

            List<Map<String, Object>> data = orderMapper.aggregateStateSales();
            List<StateSalesHeatmapDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                StateSalesHeatmapDTO dto = new StateSalesHeatmapDTO();
                dto.setState((String) row.get("state"));
                Object salesObj = row.get("sales");
                if (salesObj instanceof BigDecimal) {
                    dto.setSales((BigDecimal) salesObj);
                } else if (salesObj instanceof Number) {
                    dto.setSales(BigDecimal.valueOf(((Number) salesObj).doubleValue()));
                } else {
                    dto.setSales(BigDecimal.ZERO);
                }
                result.add(dto);
            }

            cachedStateSalesHeatmap = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }

    @Override
    public List<StateOrderCountDTO> getStateOrderCount(int topN) {
        if (topN <= 0) {
            topN = 10;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedStateOrderCount != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedStateOrderCount.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedStateOrderCount != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedStateOrderCount.stream().limit(finalTopN).collect(Collectors.toList());
            }

            List<Map<String, Object>> data = orderMapper.aggregateStateOrderCount();
            List<StateOrderCountDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                StateOrderCountDTO dto = new StateOrderCountDTO();
                dto.setState((String) row.get("state"));
                Object orderCountObj = row.get("orderCount");
                dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
                result.add(dto);
            }

            cachedStateOrderCount = result;
            cachedAtMillis = System.currentTimeMillis();
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<StateAverageOrderValueDTO> getStateAverageOrderValue(int topN) {
        if (topN <= 0) {
            topN = 10;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedStateAverageOrderValue != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedStateAverageOrderValue.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedStateAverageOrderValue != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedStateAverageOrderValue.stream().limit(finalTopN).collect(Collectors.toList());
            }

            List<Map<String, Object>> data = orderMapper.aggregateStateAverageOrderValue();
            List<StateAverageOrderValueDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                StateAverageOrderValueDTO dto = new StateAverageOrderValueDTO();
                dto.setState((String) row.get("state"));
                Object avgObj = row.get("averageOrderValue");
                if (avgObj instanceof BigDecimal) {
                    dto.setAverageOrderValue(((BigDecimal) avgObj).setScale(2, RoundingMode.HALF_UP));
                } else if (avgObj instanceof Number) {
                    dto.setAverageOrderValue(BigDecimal.valueOf(((Number) avgObj).doubleValue()).setScale(2, RoundingMode.HALF_UP));
                } else {
                    dto.setAverageOrderValue(BigDecimal.ZERO);
                }
                result.add(dto);
            }

            cachedStateAverageOrderValue = result;
            cachedAtMillis = System.currentTimeMillis();
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<StateRepurchaseRateDTO> getStateRepurchaseRate(int topN) {
        if (topN <= 0) {
            topN = 10;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedStateRepurchaseRate != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedStateRepurchaseRate.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedStateRepurchaseRate != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedStateRepurchaseRate.stream().limit(finalTopN).collect(Collectors.toList());
            }

            List<Map<String, Object>> data = orderMapper.aggregateStateRepurchaseRate();
            List<StateRepurchaseRateDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                StateRepurchaseRateDTO dto = new StateRepurchaseRateDTO();
                dto.setState((String) row.get("state"));
                Object totalCustomersObj = row.get("totalCustomers");
                Object repurchaseCustomersObj = row.get("repurchaseCustomers");
                long totalCustomers = totalCustomersObj instanceof Number ? ((Number) totalCustomersObj).longValue() : 0L;
                long repurchaseCustomers = repurchaseCustomersObj instanceof Number ? ((Number) repurchaseCustomersObj).longValue() : 0L;
                dto.setTotalCustomers(totalCustomers);
                dto.setRepurchaseCustomers(repurchaseCustomers);
                if (totalCustomers > 0) {
                    double rate = (double) repurchaseCustomers / totalCustomers;
                    dto.setRepurchaseRate(new BigDecimal(rate).setScale(4, RoundingMode.HALF_UP).doubleValue());
                } else {
                    dto.setRepurchaseRate(0.0);
                }
                result.add(dto);
            }

            cachedStateRepurchaseRate = result;
            cachedAtMillis = System.currentTimeMillis();
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<LogisticsEfficiencyDTO> getLogisticsEfficiency() {
        // 检查缓存
        if (cachedLogisticsEfficiency != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedLogisticsEfficiency;
        }

        synchronized (calculationLock) {
            if (cachedLogisticsEfficiency != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedLogisticsEfficiency;
            }

            List<Map<String, Object>> data = orderMapper.aggregateStateLogisticsEfficiency();
            List<LogisticsEfficiencyDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                LogisticsEfficiencyDTO dto = new LogisticsEfficiencyDTO();
                dto.setState((String) row.get("state"));
                Object avgDaysObj = row.get("averageDeliveryDays");
                Object orderCountObj = row.get("orderCount");
                if (avgDaysObj instanceof Number) {
                    dto.setAverageDeliveryDays(((Number) avgDaysObj).doubleValue());
                } else {
                    dto.setAverageDeliveryDays(0.0);
                }
                dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
                result.add(dto);
            }

            cachedLogisticsEfficiency = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }

    @Override
    public List<GeoUserGrowthDTO> getGeoUserGrowth() {
        // 检查缓存
        if (cachedGeoUserGrowth != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedGeoUserGrowth;
        }

        synchronized (calculationLock) {
            if (cachedGeoUserGrowth != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedGeoUserGrowth;
            }

            List<Map<String, Object>> data = orderMapper.aggregateGeoUserGrowth();
            List<GeoUserGrowthDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                GeoUserGrowthDTO dto = new GeoUserGrowthDTO();
                dto.setState((String) row.get("state"));
                dto.setMonth((String) row.get("month"));
                Object newCustomersObj = row.get("newCustomers");
                dto.setNewCustomers(newCustomersObj instanceof Number ? ((Number) newCustomersObj).longValue() : 0L);
                result.add(dto);
            }

            cachedGeoUserGrowth = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }
}
