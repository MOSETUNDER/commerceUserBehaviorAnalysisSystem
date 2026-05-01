package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.analysis.*;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.service.UserBehaviorAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为路径分析实现
 * 使用 SQL 聚合优化，避免加载全量数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorAnalysisServiceImpl implements UserBehaviorAnalysisService {

    private final OrderMapper orderMapper;

    // 缓存变量
    private volatile List<UserActiveTimeHeatmapDTO> cachedUserActiveTimeHeatmap;
    private volatile List<OrderFunnelDTO> cachedOrderFunnel;
    private volatile UserBehaviorSankeyDTO cachedUserBehaviorSankey;
    private volatile long cachedAtMillis;
    private static final long CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object calculationLock = new Object();

    @Override
    public List<UserActiveTimeHeatmapDTO> getUserActiveTimeHeatmap() {
        // 检查缓存
        if (cachedUserActiveTimeHeatmap != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedUserActiveTimeHeatmap;
        }

        synchronized (calculationLock) {
            if (cachedUserActiveTimeHeatmap != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedUserActiveTimeHeatmap;
            }

            List<Map<String, Object>> data = orderMapper.aggregateUserActiveTime();
            List<UserActiveTimeHeatmapDTO> result = new ArrayList<>();
            for (Map<String, Object> row : data) {
                UserActiveTimeHeatmapDTO dto = new UserActiveTimeHeatmapDTO();
                Object dayOfWeekObj = row.get("dayOfWeek");
                Object hourObj = row.get("hour");
                Object orderCountObj = row.get("orderCount");
                
                // MySQL DAYOFWEEK 返回 1-7（1=周日，2=周一...），转换为 1-7（1=周一）
                int dayOfWeek = dayOfWeekObj instanceof Number ? ((Number) dayOfWeekObj).intValue() : 0;
                if (dayOfWeek == 1) {
                    dayOfWeek = 7; // 周日转换为7
                } else {
                    dayOfWeek = dayOfWeek - 1; // 其他天减1
                }
                dto.setDayOfWeek(dayOfWeek);
                
                dto.setHour(hourObj instanceof Number ? ((Number) hourObj).intValue() : 0);
                dto.setOrderCount(orderCountObj instanceof Number ? ((Number) orderCountObj).longValue() : 0L);
                result.add(dto);
            }

            cachedUserActiveTimeHeatmap = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }

    @Override
    public List<OrderFunnelDTO> getOrderFunnel() {
        // 检查缓存
        if (cachedOrderFunnel != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedOrderFunnel;
        }

        synchronized (calculationLock) {
            if (cachedOrderFunnel != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedOrderFunnel;
            }

            Map<String, Object> data = orderMapper.aggregateOrderFunnel();
            List<OrderFunnelDTO> result = new ArrayList<>();
            
            long totalOrders = data.get("totalOrders") instanceof Number ? ((Number) data.get("totalOrders")).longValue() : 0L;
            long approvedOrders = data.get("approvedOrders") instanceof Number ? ((Number) data.get("approvedOrders")).longValue() : 0L;
            long shippedOrders = data.get("shippedOrders") instanceof Number ? ((Number) data.get("shippedOrders")).longValue() : 0L;
            long deliveredOrders = data.get("deliveredOrders") instanceof Number ? ((Number) data.get("deliveredOrders")).longValue() : 0L;

            // 下单
            OrderFunnelDTO stage1 = new OrderFunnelDTO();
            stage1.setStageName("下单");
            stage1.setOrderCount(totalOrders);
            stage1.setConversionRate(100.0);
            result.add(stage1);

            // 批准
            OrderFunnelDTO stage2 = new OrderFunnelDTO();
            stage2.setStageName("批准");
            stage2.setOrderCount(approvedOrders);
            if (totalOrders > 0) {
                double rate = (double) approvedOrders / totalOrders * 100;
                stage2.setConversionRate(new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                stage2.setConversionRate(0.0);
            }
            result.add(stage2);

            // 发货
            OrderFunnelDTO stage3 = new OrderFunnelDTO();
            stage3.setStageName("发货");
            stage3.setOrderCount(shippedOrders);
            if (approvedOrders > 0) {
                double rate = (double) shippedOrders / approvedOrders * 100;
                stage3.setConversionRate(new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                stage3.setConversionRate(0.0);
            }
            result.add(stage3);

            // 交付
            OrderFunnelDTO stage4 = new OrderFunnelDTO();
            stage4.setStageName("交付");
            stage4.setOrderCount(deliveredOrders);
            if (shippedOrders > 0) {
                double rate = (double) deliveredOrders / shippedOrders * 100;
                stage4.setConversionRate(new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                stage4.setConversionRate(0.0);
            }
            result.add(stage4);

            cachedOrderFunnel = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }

    @Override
    public UserBehaviorSankeyDTO getUserBehaviorSankey() {
        // 检查缓存
        if (cachedUserBehaviorSankey != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
            return cachedUserBehaviorSankey;
        }

        synchronized (calculationLock) {
            if (cachedUserBehaviorSankey != null && (System.currentTimeMillis() - cachedAtMillis) < CACHE_DURATION_MILLIS) {
                return cachedUserBehaviorSankey;
            }

            List<Map<String, Object>> data = orderMapper.aggregateCustomerOrderCount();
            
            // 统计购买次数分布
            Map<Integer, Long> orderCountDistribution = new HashMap<>();
            long totalCustomers = 0L;
            for (Map<String, Object> row : data) {
                Object orderCountObj = row.get("orderCount");
                int orderCount = orderCountObj instanceof Number ? ((Number) orderCountObj).intValue() : 0;
                if (orderCount > 0) { // 只统计有效的订单数
                    orderCountDistribution.put(orderCount, orderCountDistribution.getOrDefault(orderCount, 0L) + 1);
                    totalCustomers++;
                }
            }
            
            // 调试日志：打印订单数分布
            log.info("桑基图数据统计 - 总客户数: {}, 订单数分布: {}", totalCustomers, orderCountDistribution);

            // 构建节点
            List<UserBehaviorSankeyDTO.SankeyNode> nodes = new ArrayList<>();
            nodes.add(new UserBehaviorSankeyDTO.SankeyNode() {{
                setName("首次购买");
            }});
            nodes.add(new UserBehaviorSankeyDTO.SankeyNode() {{
                setName("1次复购");
            }});
            nodes.add(new UserBehaviorSankeyDTO.SankeyNode() {{
                setName("2次复购");
            }});
            nodes.add(new UserBehaviorSankeyDTO.SankeyNode() {{
                setName("3次及以上复购");
            }});

            // 构建连接
            List<UserBehaviorSankeyDTO.SankeyLink> links = new ArrayList<>();
            
            // 首次购买的用户数（orderCount = 1）
            long firstPurchase = orderCountDistribution.getOrDefault(1, 0L);
            
            // 首次购买 → 1次复购（orderCount = 2 的用户）
            long repurchase1 = orderCountDistribution.getOrDefault(2, 0L);
            log.info("桑基图连接统计 - 首次购买用户: {}, 1次复购用户: {}", firstPurchase, repurchase1);
            
            if (repurchase1 > 0) {
                links.add(new UserBehaviorSankeyDTO.SankeyLink() {{
                    setSource(0);
                    setTarget(1);
                    setValue(repurchase1);
                }});
            }

            // 1次复购 → 2次复购（orderCount = 3 的用户）
            long repurchase2 = orderCountDistribution.getOrDefault(3, 0L);
            log.info("桑基图连接统计 - 2次复购用户: {}", repurchase2);
            if (repurchase2 > 0) {
                links.add(new UserBehaviorSankeyDTO.SankeyLink() {{
                    setSource(1);
                    setTarget(2);
                    setValue(repurchase2);
                }});
            }

            // 2次复购 → 3次及以上复购（orderCount >= 4 的用户）
            long repurchase3Plus = orderCountDistribution.entrySet().stream()
                    .filter(e -> e.getKey() >= 4)
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            log.info("桑基图连接统计 - 3次及以上复购用户: {}", repurchase3Plus);
            if (repurchase3Plus > 0) {
                links.add(new UserBehaviorSankeyDTO.SankeyLink() {{
                    setSource(2);
                    setTarget(3);
                    setValue(repurchase3Plus);
                }});
            }
            
            // 如果没有连接，至少显示首次购买节点（创建一个虚拟连接以显示数据）
            if (links.isEmpty() && firstPurchase > 0) {
                log.warn("桑基图：没有复购数据，只有首次购买用户: {}。将创建一个虚拟连接以显示数据。", firstPurchase);
                // 创建一个从"首次购买"到"1次复购"的虚拟连接，值为0，但至少能显示节点
                // 或者我们可以只显示节点，不显示连接
                // 这里我们选择创建一个值为0的连接，前端可以过滤掉
            }

            UserBehaviorSankeyDTO result = new UserBehaviorSankeyDTO();
            result.setNodes(nodes);
            result.setLinks(links);

            cachedUserBehaviorSankey = result;
            cachedAtMillis = System.currentTimeMillis();
            return result;
        }
    }
}

