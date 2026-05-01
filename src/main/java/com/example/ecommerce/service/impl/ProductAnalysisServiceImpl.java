package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.analysis.*;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.service.ProductAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品 / 品类相关分析实现
 * 使用 SQL 聚合优化，避免加载全量数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAnalysisServiceImpl implements ProductAnalysisService {

    private final OrderMapper orderMapper;

    // 缓存变量
    private volatile List<CategorySalesDTO> cachedTopCategorySales;
    private volatile List<CategoryRepurchaseHeatmapDTO> cachedCategoryRepurchaseHeatmap;
    private volatile List<ProductSalesDTO> cachedTopProductSales;
    private volatile List<CategoryStateSalesDTO> cachedCategoryStateSales;
    private volatile long cachedAtMillis;
    private static final long CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object calculationLock = new Object();

    @Override
    public List<CategorySalesDTO> getTopCategorySales(int topN) {
        if (topN <= 0) {
            topN = 10;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedTopCategorySales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedTopCategorySales.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedTopCategorySales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedTopCategorySales.stream().limit(finalTopN).collect(Collectors.toList());
            }

            List<Map<String, Object>> data = orderMapper.aggregateTopCategorySales(); // 查询 Top 50
            List<CategorySalesDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                CategorySalesDTO dto = new CategorySalesDTO();
                dto.setCategoryName((String) row.get("categoryName"));
                Object orderCountObj = row.get("orderCount");
                dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
                Object salesAmountObj = row.get("salesAmount");
                if (salesAmountObj instanceof BigDecimal) {
                    dto.setTotalAmount((BigDecimal) salesAmountObj);
                } else if (salesAmountObj instanceof Number) {
                    dto.setTotalAmount(BigDecimal.valueOf(((Number) salesAmountObj).doubleValue()));
                } else {
                    dto.setTotalAmount(BigDecimal.ZERO);
                }
                result.add(dto);
            }

            cachedTopCategorySales = result;
            cachedAtMillis = System.currentTimeMillis();
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<CategoryRepurchaseHeatmapDTO> getCategoryRepurchaseHeatmap() {
        // 检查缓存
        if (cachedCategoryRepurchaseHeatmap != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedCategoryRepurchaseHeatmap;
        }

        synchronized (calculationLock) {
            if (cachedCategoryRepurchaseHeatmap != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedCategoryRepurchaseHeatmap;
            }

            List<Map<String, Object>> data = orderMapper.aggregateCategoryRepurchaseHeatmap();
            List<CategoryRepurchaseHeatmapDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                CategoryRepurchaseHeatmapDTO dto = new CategoryRepurchaseHeatmapDTO();
                dto.setCategoryName((String) row.get("categoryName"));
                dto.setSegmentLabel((String) row.get("segmentLabel"));
                Object repurchaseCustomersObj = row.get("repurchaseCustomers");
                Object totalCustomersObj = row.get("totalCustomers");
                long repurchaseCustomers = repurchaseCustomersObj instanceof Number ? ((Number) repurchaseCustomersObj).longValue() : 0L;
                long totalCustomers = totalCustomersObj instanceof Number ? ((Number) totalCustomersObj).longValue() : 0L;
                dto.setCustomerCount(totalCustomers);
                if (totalCustomers > 0) {
                    double rate = (double) repurchaseCustomers / totalCustomers;
                    dto.setRepurchaseRate(new BigDecimal(rate).setScale(4, RoundingMode.HALF_UP).doubleValue());
                } else {
                    dto.setRepurchaseRate(0.0);
                }
                result.add(dto);
            }

            cachedCategoryRepurchaseHeatmap = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }

    @Override
    public List<ProductSalesDTO> getTopProductSales(int topN) {
        if (topN <= 0) {
            topN = 20;
        }
        final int finalTopN = topN;

        // 检查缓存
        if (cachedTopProductSales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedTopProductSales.stream().limit(finalTopN).collect(Collectors.toList());
        }

        synchronized (calculationLock) {
            if (cachedTopProductSales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedTopProductSales.stream().limit(finalTopN).collect(Collectors.toList());
            }

            List<Map<String, Object>> data = orderMapper.aggregateTopProductSales(); // 查询 Top 100
            List<ProductSalesDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                ProductSalesDTO dto = new ProductSalesDTO();
                dto.setProductId((String) row.get("productId"));
                dto.setCategoryName((String) row.get("categoryName"));
                Object salesCountObj = row.get("salesCount");
                dto.setSalesCount(salesCountObj instanceof Number ? ((Number) salesCountObj).longValue() : 0L);
                Object salesAmountObj = row.get("salesAmount");
                if (salesAmountObj instanceof BigDecimal) {
                    dto.setSalesAmount((BigDecimal) salesAmountObj);
                } else if (salesAmountObj instanceof Number) {
                    dto.setSalesAmount(BigDecimal.valueOf(((Number) salesAmountObj).doubleValue()));
                } else {
                    dto.setSalesAmount(BigDecimal.ZERO);
                }
                result.add(dto);
            }

            cachedTopProductSales = result;
            cachedAtMillis = System.currentTimeMillis();
            return result.stream().limit(finalTopN).collect(Collectors.toList());
        }
    }

    @Override
    public List<CategoryStateSalesDTO> getCategoryStateSales() {
        // 检查缓存
        if (cachedCategoryStateSales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedCategoryStateSales;
        }

        synchronized (calculationLock) {
            if (cachedCategoryStateSales != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedCategoryStateSales;
            }

            List<Map<String, Object>> data = orderMapper.aggregateCategoryStateSales();
            List<CategoryStateSalesDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                CategoryStateSalesDTO dto = new CategoryStateSalesDTO();
                dto.setCategoryName((String) row.get("categoryName"));
                dto.setState((String) row.get("state"));
                Object orderCountObj = row.get("orderCount");
                dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
                Object salesAmountObj = row.get("salesAmount");
                if (salesAmountObj instanceof BigDecimal) {
                    dto.setSalesAmount((BigDecimal) salesAmountObj);
                } else if (salesAmountObj instanceof Number) {
                    dto.setSalesAmount(BigDecimal.valueOf(((Number) salesAmountObj).doubleValue()));
                } else {
                    dto.setSalesAmount(BigDecimal.ZERO);
                }
                result.add(dto);
            }

            cachedCategoryStateSales = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }
}
