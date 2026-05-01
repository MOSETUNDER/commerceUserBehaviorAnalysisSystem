package com.example.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerce.dto.analysis.ChurnStatsDTO;
import com.example.ecommerce.dto.analysis.RfmCustomerScoreDTO;
import com.example.ecommerce.dto.analysis.RfmRadarChartDTO;
import com.example.ecommerce.dto.analysis.SegmentDistributionDTO;
import com.example.ecommerce.dto.analysis.TopCustomerDTO;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderPayment;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.mapper.OrderPaymentMapper;
import com.example.ecommerce.service.CustomerAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Randomizable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * 客户 / 用户相关分析实现（RFM、分层、流失、高价值、复购预测）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAnalysisServiceImpl implements CustomerAnalysisService {

    private final OrderMapper orderMapper;
    private final OrderPaymentMapper orderPaymentMapper;

    /**
     * RFM 结果简单缓存，避免每个请求都全量扫描订单与支付表
     * 缓存时间延长到 10 分钟，减少数据库查询压力
     */
    private volatile List<RfmCustomerScoreDTO> cachedRfmList;
    private volatile LocalDate cachedAsOfDate;
    private volatile long cachedAtMillis;
    private static final long RFM_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟

    /**
     * 随机森林模型与数据结构（简单内存缓存，真实项目可考虑持久化模型文件）
     */
    private volatile RandomForest repurchaseModel;
    private volatile Instances repurchaseHeader;
    
    // 复购概率分布缓存
    private volatile List<com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO> cachedRepurchaseProbDistribution;
    private volatile long cachedRepurchaseProbDistributionAtMillis;
    private static final long REPURCHASE_PROB_DISTRIBUTION_CACHE_DURATION_MILLIS = 30 * 60 * 1000; // 30 分钟缓存

    /**
     * RFM 计算同步锁，确保同一时间只有一个请求在计算，避免重复计算
     */
    private final Object rfmCalculationLock = new Object();

    @Override
    public List<RfmCustomerScoreDTO> calculateAllCustomerRfm(LocalDate asOfDate) {
        long startTime = System.currentTimeMillis();
        try {
            // 先检查缓存（不加锁，快速路径）
            LocalDate maxOrderDate = orderMapper.getMaxOrderDate();
            if (maxOrderDate == null) {
                maxOrderDate = LocalDate.of(2018, 10, 1);
            }

            // 固定参考日期为 2018-10-01（Olist 数据集标准参考日期）
            LocalDate referenceDate;
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(maxOrderDate, LocalDate.of(2018, 10, 1));
            if (Math.abs(daysBetween) <= 30) {
                // 数据日期接近 2018-10-01，使用标准参考日期
                referenceDate = LocalDate.of(2018, 10, 1);
            } else if (maxOrderDate.isBefore(LocalDate.of(2018, 10, 1))) {
                // 历史数据，使用数据的最新日期
                referenceDate = maxOrderDate;
            } else {
                // 如果数据是未来的，使用传入的 asOfDate，否则固定为 2018-10-01
                referenceDate = (asOfDate != null) ? asOfDate : LocalDate.of(2018, 10, 1);
            }

            List<RfmCustomerScoreDTO> snapshot = cachedRfmList;
            if (snapshot != null
                    && referenceDate.equals(cachedAsOfDate)
                    && (System.currentTimeMillis() - cachedAtMillis) < RFM_CACHE_DURATION_MILLIS) {
                log.info("RFM 计算：使用缓存，耗时 {}ms", System.currentTimeMillis() - startTime);
                return snapshot;
            }

            // 加锁，确保同一时间只有一个请求在计算
            synchronized (rfmCalculationLock) {
                // 双重检查，可能其他线程已经计算完成
                snapshot = cachedRfmList;
                if (snapshot != null
                        && referenceDate.equals(cachedAsOfDate)
                        && (System.currentTimeMillis() - cachedAtMillis) < RFM_CACHE_DURATION_MILLIS) {
                    log.info("RFM 计算：使用缓存（加锁后检查），耗时 {}ms", System.currentTimeMillis() - startTime);
                    return snapshot;
                }

                // 执行计算
                return doCalculateRfm(referenceDate, startTime);
            }
        } catch (Exception e) {
            log.error("RFM 计算失败，耗时：{}ms", System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    /**
     * 执行实际的 RFM 计算
     */
    private List<RfmCustomerScoreDTO> doCalculateRfm(LocalDate referenceDate, long startTime) {
        try {
            // 使用优化的 SQL 聚合查询，直接在数据库层面计算 RFM 基础数据
            long sqlStart = System.currentTimeMillis();
            List<Map<String, Object>> aggregatedData = orderMapper.aggregateCustomerRfmData();
            log.info("RFM SQL 查询耗时：{}ms，返回 {} 条记录", System.currentTimeMillis() - sqlStart, 
                    aggregatedData != null ? aggregatedData.size() : 0);
            
            if (CollectionUtils.isEmpty(aggregatedData)) {
                return Collections.emptyList();
            }

            long dataProcessStart = System.currentTimeMillis();
            List<RfmCustomerScoreDTO> customerScores = new ArrayList<>();
            for (Map<String, Object> row : aggregatedData) {
                // SQL 查询现在返回 customerUniqueId，但 DTO 中字段名是 customerId
                // 这里保持兼容，将 customerUniqueId 存储到 customerId 字段
                String customerId = (String) row.get("customerUniqueId");
                if (customerId == null) {
                    // 兼容旧数据格式
                    customerId = (String) row.get("customerId");
                }
                LocalDate lastOrderDate = null;
                Object dateObj = row.get("lastOrderDate");
                if (dateObj != null) {
                    if (dateObj instanceof java.sql.Date) {
                        lastOrderDate = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof LocalDate) {
                        lastOrderDate = (LocalDate) dateObj;
                    } else if (dateObj instanceof java.util.Date) {
                        lastOrderDate = ((java.util.Date) dateObj).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                    }
                }
                
                Number orderCountNum = (Number) row.get("orderCount");
                int orderCount = orderCountNum != null ? orderCountNum.intValue() : 0;
                
                Number amountNum = (Number) row.get("totalAmount");
                BigDecimal totalAmount = amountNum != null 
                        ? BigDecimal.valueOf(amountNum.doubleValue())
                        : BigDecimal.ZERO;

                RfmCustomerScoreDTO dto = new RfmCustomerScoreDTO();
                dto.setCustomerId(customerId);
                dto.setLastOrderDate(lastOrderDate);
                dto.setOrderCount(orderCount);
                dto.setTotalAmount(totalAmount);
                customerScores.add(dto);
            }
            log.info("RFM 数据转换耗时：{}ms", System.currentTimeMillis() - dataProcessStart);

            // 按照工具类标准：使用五分位数计算 R/F/M 分数（1-5分）
            // 优化：先计算所有 recencyDays，然后一次性排序和计算分位数阈值
            long quintileStart = System.currentTimeMillis();
            
            // 1. 收集所有值并计算 recencyDays
            List<Long> recencyDaysList = new ArrayList<>(customerScores.size());
            List<Integer> frequencyList = new ArrayList<>(customerScores.size());
            List<Double> monetaryList = new ArrayList<>(customerScores.size());
            
            for (RfmCustomerScoreDTO dto : customerScores) {
                long recencyDays = dto.getLastOrderDate() == null
                        ? Integer.MAX_VALUE
                        : java.time.temporal.ChronoUnit.DAYS.between(dto.getLastOrderDate(), referenceDate);
                recencyDaysList.add(recencyDays);
                frequencyList.add(dto.getOrderCount());
                monetaryList.add(dto.getTotalAmount().doubleValue());
            }
            
            // 2. 排序并计算分位数阈值（只排序一次）
            recencyDaysList.sort(Long::compareTo);
            frequencyList.sort(Integer::compareTo);
            monetaryList.sort(Double::compareTo);
            
            int n = customerScores.size();
            // 预计算分位数阈值，避免每次查找
            long[] recencyThresholds = new long[4]; // 20%, 40%, 60%, 80%
            int[] freqThresholds = new int[4];
            double[] monetaryThresholds = new double[4];
            
            for (int i = 0; i < 4; i++) {
                int index = (int) Math.floor((i + 1) * 0.2 * n);
                if (index >= n) index = n - 1;
                recencyThresholds[i] = recencyDaysList.get(index);
                freqThresholds[i] = frequencyList.get(index);
                monetaryThresholds[i] = monetaryList.get(index);
            }
            
            log.info("RFM 排序和阈值计算耗时：{}ms", System.currentTimeMillis() - quintileStart);
            
            // 调试：打印分位数阈值
            log.info("=== RFM 分位数阈值 ===");
            log.info("Recency 阈值（越小越好）: 20%={}, 40%={}, 60%={}, 80%={}", 
                    recencyThresholds[0], recencyThresholds[1], recencyThresholds[2], recencyThresholds[3]);
            log.info("Frequency 阈值（越大越好）: 20%={}, 40%={}, 60%={}, 80%={}", 
                    freqThresholds[0], freqThresholds[1], freqThresholds[2], freqThresholds[3]);
            log.info("Monetary 阈值（越大越好）: 20%={}, 40%={}, 60%={}, 80%={}", 
                    monetaryThresholds[0], monetaryThresholds[1], monetaryThresholds[2], monetaryThresholds[3]);

            // 3. 为每个客户计算五分位数分数（1-5分），使用预计算的阈值
            long scoreCalcStart = System.currentTimeMillis();
            
            // 调试：统计各分数段的分布
            Map<String, Integer> segmentCountMap = new HashMap<>();
            Map<String, Integer> rScoreDist = new HashMap<>();
            Map<String, Integer> fScoreDist = new HashMap<>();
            Map<String, Integer> mScoreDist = new HashMap<>();
            
            for (RfmCustomerScoreDTO dto : customerScores) {
                long recencyDays = dto.getLastOrderDate() == null
                        ? Integer.MAX_VALUE
                        : java.time.temporal.ChronoUnit.DAYS.between(dto.getLastOrderDate(), referenceDate);
                
                // 保存 recencyDays 到 DTO，供复购预测使用
                dto.setRecencyDays((int) recencyDays);
                
                // 使用预计算的阈值快速确定分数
                // 按照工具类 RFMSegmentationFromCSV 的标准：R/F/M 都使用五分位数计算
                int rScore = getQuintileScoreFast(recencyDays, recencyThresholds, true);
                int fScore = getQuintileScoreFast(dto.getOrderCount(), freqThresholds, false);
                int mScore = getQuintileScoreFast(dto.getTotalAmount().doubleValue(), monetaryThresholds, false);

                dto.setRScore(rScore);
                dto.setFScore(fScore);
                dto.setMScore(mScore);

                // RFM 综合评分（可选，用于排序）
                double rfmScore = 0.3 * rScore + 0.3 * fScore + 0.4 * mScore;
                dto.setRfmScore(round2(rfmScore));
                
                // 使用经典 RFM 分层规则
                dto.setSegmentLabel(buildClassicSegmentLabel(rScore, fScore, mScore));
                
                // 调试：统计分布
                segmentCountMap.merge(dto.getSegmentLabel(), 1, Integer::sum);
                rScoreDist.merge("R=" + rScore, 1, Integer::sum);
                fScoreDist.merge("F=" + fScore, 1, Integer::sum);
                mScoreDist.merge("M=" + mScore, 1, Integer::sum);
            }
            log.info("RFM 分数计算耗时：{}ms", System.currentTimeMillis() - scoreCalcStart);
            
            // 调试：打印统计信息
            log.info("=== RFM Segment 分布 ===");
            segmentCountMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> log.info("  {}: {}", entry.getKey(), entry.getValue()));
            
            log.info("=== R 分数分布 ===");
            rScoreDist.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> log.info("  {}: {}", entry.getKey(), entry.getValue()));
            
            log.info("=== F 分数分布 ===");
            fScoreDist.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> log.info("  {}: {}", entry.getKey(), entry.getValue()));
            
            // 详细统计 F 分分布（确认 F 分分布情况）
            Map<Integer, Long> fDistDetail = customerScores.stream()
                    .collect(Collectors.groupingBy(RfmCustomerScoreDTO::getFScore, Collectors.counting()));
            log.info("F 分详细分布: {}", fDistDetail);
            
            log.info("=== M 分数分布 ===");
            mScoreDist.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> log.info("  {}: {}", entry.getKey(), entry.getValue()));
            
            // 调试：打印前10个客户的详细 RFM 数据
            log.info("=== 前10个客户 RFM 详细数据 ===");
            customerScores.stream().limit(10).forEach(dto -> {
                log.info("客户ID: {}, R={}, F={}, M={}, Segment={}, RecencyDays={}, OrderCount={}, TotalAmount={}", 
                        dto.getCustomerId(), dto.getRScore(), dto.getFScore(), dto.getMScore(),
                        dto.getSegmentLabel(), dto.getRecencyDays(), dto.getOrderCount(), dto.getTotalAmount());
            });
            
            // 调试：打印各 segment 的样本客户
            log.info("=== 各 Segment 样本客户（每个取前3个）===");
            Map<String, List<RfmCustomerScoreDTO>> segmentGroups = customerScores.stream()
                    .collect(Collectors.groupingBy(RfmCustomerScoreDTO::getSegmentLabel));
            segmentGroups.forEach((segment, list) -> {
                log.info("Segment: {} (共{}个)", segment, list.size());
                list.stream().limit(3).forEach(dto -> {
                    log.info("  - 客户ID: {}, R={}, F={}, M={}, RecencyDays={}, OrderCount={}, TotalAmount={}", 
                            dto.getCustomerId(), dto.getRScore(), dto.getFScore(), dto.getMScore(),
                            dto.getRecencyDays(), dto.getOrderCount(), dto.getTotalAmount());
                });
            });

            // 写入缓存
            cachedRfmList = customerScores;
            cachedAsOfDate = referenceDate;
            cachedAtMillis = System.currentTimeMillis();

            log.info("RFM 计算完成，总耗时：{}ms，客户数：{}", 
                    System.currentTimeMillis() - startTime, customerScores.size());
            return customerScores;
        } catch (Exception e) {
            log.error("RFM 计算失败，耗时：{}ms", System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    @Override
    public List<SegmentDistributionDTO> getRfmSegmentDistribution(LocalDate asOfDate) {
        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(asOfDate);
        if (CollectionUtils.isEmpty(all)) {
            return Collections.emptyList();
        }

        Map<String, Long> map = new LinkedHashMap<>();
        // 先放入四个固定的中文分类，保证前端总是有四块饼
        map.put("高价值客户", 0L);
        map.put("潜力客户", 0L);
        map.put("一般客户", 0L);
        map.put("流失客户", 0L);

        for (RfmCustomerScoreDTO dto : all) {
            String bucket = toChineseBucket(dto.getSegmentLabel());
            map.merge(bucket, 1L, Long::sum);
        }

        List<SegmentDistributionDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            SegmentDistributionDTO dto = new SegmentDistributionDTO();
            dto.setSegmentLabel(entry.getKey());
            dto.setCount(entry.getValue());
            result.add(dto);
        }
        return result;
    }

    /**
     * 将经典 RFM 英文标签聚合为中文四大类：
     * 高价值客户：Champions / Loyal Customers / Cannot Lose Them
     * 潜力客户：Potential Loyalists / Recent Customers / Promising
     * 一般客户：Other / At Risk（风险但未流失）
     * 流失客户：Hibernating / Lost
     */
    private String toChineseBucket(String classicSegment) {
        if (classicSegment == null) {
            return "一般客户";
        }
        switch (classicSegment) {
            case "Champions":
            case "Loyal Customers":
            case "Cannot Lose Them":
                return "高价值客户";
            case "Potential Loyalists":
            case "Recent Customers":
            case "Promising":
                return "潜力客户";
            case "Hibernating":
            case "Lost":
            case "At Risk":  // 有流失风险的客户也应该归类为流失客户
                return "流失客户";
            default:
                // 包含 Other 等
                return "一般客户";
        }
    }

    @Override
    public RfmRadarChartDTO getCustomerRfmRadar(String customerId, LocalDate asOfDate) {
        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(asOfDate);
        RfmCustomerScoreDTO target = all.stream()
                .filter(c -> Objects.equals(c.getCustomerId(), customerId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return new RfmRadarChartDTO();
        }

        RfmRadarChartDTO.RadarIndicator r = new RfmRadarChartDTO.RadarIndicator();
        r.setName("R");
        r.setMax(5.0); // RFM 分数范围是 1-5，所以 max 应该是 5.0

        RfmRadarChartDTO.RadarIndicator f = new RfmRadarChartDTO.RadarIndicator();
        f.setName("F");
        f.setMax(5.0); // RFM 分数范围是 1-5，所以 max 应该是 5.0

        RfmRadarChartDTO.RadarIndicator m = new RfmRadarChartDTO.RadarIndicator();
        m.setName("M");
        m.setMax(5.0); // RFM 分数范围是 1-5，所以 max 应该是 5.0

        RfmRadarChartDTO.RadarDataItem item = new RfmRadarChartDTO.RadarDataItem();
        item.setName("客户 " + customerId);
        // 将 int 类型的 R/F/M 分数转换为 Double 用于雷达图
        item.setValue(Arrays.asList(
                (double) target.getRScore(), 
                (double) target.getFScore(), 
                (double) target.getMScore()
        ));

        RfmRadarChartDTO chartDTO = new RfmRadarChartDTO();
        chartDTO.setIndicators(Arrays.asList(r, f, m));
        chartDTO.setData(Collections.singletonList(item));
        return chartDTO;
    }

    @Override
    public ChurnStatsDTO getChurnStats(LocalDate asOfDate, int activeDays, int sleepingDays) {
        // 固定参考日期为 2018-10-01（Olist 数据集标准参考日期）
        // 避免使用当前日期导致历史数据被误判为流失
        if (asOfDate == null) {
            asOfDate = LocalDate.of(2018, 10, 1);
        }
        if (activeDays <= 0) {
            activeDays = 30;
        }
        if (sleepingDays <= activeDays) {
            sleepingDays = activeDays * 3;
        }

        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(asOfDate);
        ChurnStatsDTO stats = new ChurnStatsDTO();
        
        // 使用固定的参考日期（2018-10-01）计算流失，而不是当前日期
        // 这样流失统计是基于历史数据的相对时间，而不是绝对时间
        LocalDate referenceDate = asOfDate;
        
        for (RfmCustomerScoreDTO dto : all) {
            if (dto.getLastOrderDate() == null) {
                stats.setChurnedCount(stats.getChurnedCount() + 1);
                continue;
            }
            long diff = java.time.temporal.ChronoUnit.DAYS.between(dto.getLastOrderDate(), referenceDate);
            if (diff <= activeDays) {
                stats.setActiveCount(stats.getActiveCount() + 1);
            } else if (diff <= sleepingDays) {
                stats.setSleepingCount(stats.getSleepingCount() + 1);
            } else {
                stats.setChurnedCount(stats.getChurnedCount() + 1);
            }
        }
        return stats;
    }

    @Override
    public List<TopCustomerDTO> getTopCustomersByAmount(int topN) {
        if (topN <= 0) {
            topN = 10;
        }
        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(null);
        return all.stream()
                .sorted(Comparator.comparing(RfmCustomerScoreDTO::getTotalAmount).reversed())
                .limit(topN)
                .map(dto -> {
                    TopCustomerDTO t = new TopCustomerDTO();
                    t.setCustomerId(dto.getCustomerId());
                    t.setOrderCount(dto.getOrderCount());
                    t.setTotalAmount(dto.getTotalAmount());
                    if (dto.getOrderCount() > 0) {
                        t.setAverageOrderValue(dto.getTotalAmount()
                                .divide(BigDecimal.valueOf(dto.getOrderCount()), 2, RoundingMode.HALF_UP));
                    } else {
                        t.setAverageOrderValue(BigDecimal.ZERO);
                    }
                    return t;
                })
                .collect(Collectors.toList());
    }

    /**
     * 预测客户复购概率
     * 
     * @param customerId 客户ID，注意：这里实际使用的是 customer_unique_id（真实用户ID），
     *                    而不是系统内部的 customer_id。因为 RFM 计算和复购预测都基于真实用户ID。
     * @return 复购概率（0.0-1.0）
     */
    @Override
    public double predictCustomerRepurchaseProbability(String customerId) {
        try {
            ensureRepurchaseModel();
            if (repurchaseModel == null || repurchaseHeader == null) {
                return 0.0;
            }

            // 构造目标客户的特征向量：按照工具类标准使用 frequency, monetary, rScore, fScore, mScore
            // 注意：customerId 参数实际是 customer_unique_id（真实用户ID）
            List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(null);
            RfmCustomerScoreDTO target = all.stream()
                    .filter(c -> Objects.equals(c.getCustomerId(), customerId))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                return 0.0;
            }

            double[] values = new double[repurchaseHeader.numAttributes()];
            // 移除 recencyDays，避免数据泄露
            // 特征顺序：frequency, monetary, rScore, fScore, mScore
            values[0] = target.getOrderCount();        // frequency
            values[1] = target.getTotalAmount().doubleValue(); // monetary
            values[2] = target.getRScore();           // rScore
            values[3] = target.getFScore();           // fScore
            values[4] = target.getMScore();           // mScore
            // class 属性占位，预测时可以先置为缺失值
            values[5] = Utils.missingValue(); // 标签索引从 6 改为 5

            DenseInstance instance = new DenseInstance(1.0, values);
            instance.setDataset(repurchaseHeader);

            double[] dist = repurchaseModel.distributionForInstance(instance);
            int yesIndex = repurchaseHeader.classAttribute().indexOfValue("1"); // 复购类别是 "1"
            if (yesIndex < 0 || yesIndex >= dist.length) {
                return 0.0;
            }
            return round2(dist[yesIndex]);
        } catch (Exception e) {
            // 预测失败时不抛异常，避免影响整体接口，返回 0 作为降级
            return 0.0;
        }
    }

    /**
     * 计算五分位数分数（1-5分），与工具类 RFMSegmentationFromCSV 保持一致
     * @param value 当前值
     * @param sortedList 已排序的列表
     * @param reverse true 表示值越小越好（如 Recency），false 表示值越大越好（如 Frequency、Monetary）
     */
    private int getQuintileScore(double value, List<? extends Number> sortedList, boolean reverse) {
        if (sortedList.isEmpty()) {
            return 3; // 默认中位数
        }
        int n = sortedList.size();
        int rank = 0;
        for (int i = 0; i < n; i++) {
            double listValue = sortedList.get(i).doubleValue();
            if ((reverse && value <= listValue) || (!reverse && value >= listValue)) {
                rank = i;
                break;
            }
        }
        // 如果没找到（值超出范围），rank 保持为 0 或设为 n-1
        if (rank == 0 && reverse && sortedList.get(0).doubleValue() < value) {
            rank = n - 1; // Recency 最大，排名最后
        } else if (rank == 0 && !reverse && sortedList.get(n - 1).doubleValue() < value) {
            rank = n - 1; // Frequency/Monetary 最大，排名最后
        }
        double percentile = (double) rank / n;
        if (percentile < 0.2) return reverse ? 5 : 1;
        if (percentile < 0.4) return reverse ? 4 : 2;
        if (percentile < 0.6) return 3;
        if (percentile < 0.8) return reverse ? 2 : 4;
        return reverse ? 1 : 5;
    }

    /**
     * 快速计算五分位数分数（使用预计算的阈值）
     * @param value 当前值
     * @param thresholds 预计算的分位数阈值数组 [20%, 40%, 60%, 80%]
     * @param reverse true 表示值越小越好（如 Recency），false 表示值越大越好（如 Frequency、Monetary）
     */
    private int getQuintileScoreFast(long value, long[] thresholds, boolean reverse) {
        if (reverse) {
            // Recency: 越小越好
            if (value <= thresholds[0]) return 5; // <= 20%
            if (value <= thresholds[1]) return 4; // <= 40%
            if (value <= thresholds[2]) return 3; // <= 60%
            if (value <= thresholds[3]) return 2; // <= 80%
            return 1; // > 80%
        } else {
            // Frequency/Monetary: 越大越好
            if (value >= thresholds[3]) return 5; // >= 80%
            if (value >= thresholds[2]) return 4; // >= 60%
            if (value >= thresholds[1]) return 3; // >= 40%
            if (value >= thresholds[0]) return 2; // >= 20%
            return 1; // < 20%
        }
    }

    /**
     * 快速计算五分位数分数（使用预计算的阈值，double 版本）
     */
    private int getQuintileScoreFast(int value, int[] thresholds, boolean reverse) {
        if (reverse) {
            if (value <= thresholds[0]) return 5;
            if (value <= thresholds[1]) return 4;
            if (value <= thresholds[2]) return 3;
            if (value <= thresholds[3]) return 2;
            return 1;
        } else {
            if (value >= thresholds[3]) return 5;
            if (value >= thresholds[2]) return 4;
            if (value >= thresholds[1]) return 3;
            if (value >= thresholds[0]) return 2;
            return 1;
        }
    }

    /**
     * 快速计算五分位数分数（使用预计算的阈值，double 版本）
     */
    private int getQuintileScoreFast(double value, double[] thresholds, boolean reverse) {
        if (reverse) {
            if (value <= thresholds[0]) return 5;
            if (value <= thresholds[1]) return 4;
            if (value <= thresholds[2]) return 3;
            if (value <= thresholds[3]) return 2;
            return 1;
        } else {
            if (value >= thresholds[3]) return 5;
            if (value >= thresholds[2]) return 4;
            if (value >= thresholds[1]) return 3;
            if (value >= thresholds[0]) return 2;
            return 1;
        }
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 经典 RFM 分层规则（与工具类 RFMSegmentationFromCSV 保持一致）
     */
    private String buildClassicSegmentLabel(int r, int f, int m) {
        if (r >= 5 && f >= 4 && m >= 4) {
            return "Champions"; // 重要价值客户
        } else if (r >= 4 && f >= 4 && m >= 4) {
            return "Loyal Customers"; // 忠诚客户
        } else if (r >= 4 && f >= 3) {
            return "Potential Loyalists"; // 潜在忠诚客户
        } else if (r == 5 && f <= 2) {
            return "Recent Customers"; // 新客户
        } else if (r <= 2 && f >= 3 && m >= 3) {
            return "Cannot Lose Them"; // 不能失去的客户
        } else if (r <= 2 && f >= 2) {
            return "At Risk"; // 有流失风险
        } else if (r <= 2 && f <= 2 && m <= 2) {
            return "Hibernating"; // 沉睡客户
        } else if (r <= 1 && f <= 1 && m <= 1) {
            return "Lost"; // 流失客户
        } else {
            return "Other"; // 其他
        }
    }

    /**
     * 确保复购模型已训练（懒加载一次）。
     * 按照工具类标准：使用 recencyDays, frequency, monetary, rScore, fScore, mScore 作为特征
     * 复购标签：最后购买日期 > 2017-12-01 视为会复购（与 GenerateRepurchaseLabels 保持一致）
     */
    private synchronized void ensureRepurchaseModel() throws Exception {
        if (repurchaseModel != null && repurchaseHeader != null) {
            return;
        }

        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(null);
        if (CollectionUtils.isEmpty(all)) {
            return;
        }

        // 按照工具类 RepurchasePredictionWeka 的特征定义
        // 注意：移除 recencyDays，因为它基于参考日期 2018-10-01 计算，而标签基于 2017-12-01，
        // 导致 recencyDays 可以直接区分标签（数据泄露），所以只使用 RFM 分数
        ArrayList<Attribute> attrs = new ArrayList<>();
        // attrs.add(new Attribute("recencyDays")); // 移除：会导致数据泄露
        attrs.add(new Attribute("frequency"));
        attrs.add(new Attribute("monetary"));
        attrs.add(new Attribute("rScore"));
        attrs.add(new Attribute("fScore"));
        attrs.add(new Attribute("mScore"));
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("0"); // 不复购
        classValues.add("1"); // 复购
        Attribute classAttr = new Attribute("willRepurchase", classValues);
        attrs.add(classAttr);

        Instances dataset = new Instances("customer_repurchase", attrs, all.size());
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // 复购标签截止日期（与 GenerateRepurchaseLabels 保持一致）
        LocalDateTime cutoffDate = LocalDateTime.of(2017, 12, 1, 0, 0);

        for (RfmCustomerScoreDTO dto : all) {
            double[] vals = new double[dataset.numAttributes()];
            // 移除 recencyDays，避免数据泄露
            // 只使用 frequency, monetary, rScore, fScore, mScore 作为特征
            vals[0] = dto.getOrderCount();        // frequency
            vals[1] = dto.getTotalAmount().doubleValue(); // monetary
            vals[2] = dto.getRScore();           // rScore
            vals[3] = dto.getFScore();           // fScore
            vals[4] = dto.getMScore();           // mScore
            
            // 复购标签：最后购买日期 > 2017-12-01
            int willRepurchase = (dto.getLastOrderDate() != null 
                    && dto.getLastOrderDate().atStartOfDay().isAfter(cutoffDate)) ? 1 : 0;
            vals[5] = willRepurchase; // 标签索引从 5 改为 5（原来是 6）
            
            dataset.add(new DenseInstance(1.0, vals));
        }

        // 统计标签分布
        long repurchaseCount = 0;
        long noRepurchaseCount = 0;
        for (int i = 0; i < dataset.numInstances(); i++) {
            if (dataset.instance(i).classValue() == 1.0) {
                repurchaseCount++;
            } else {
                noRepurchaseCount++;
            }
        }
        log.info("=== 复购预测模型训练数据统计 ===");
        log.info("总样本数: {}", dataset.numInstances());
        log.info("复购用户（标签=1）: {} ({}%)", repurchaseCount, 
                String.format("%.2f", repurchaseCount * 100.0 / dataset.numInstances()));
        log.info("不复购用户（标签=0）: {} ({}%)", noRepurchaseCount,
                String.format("%.2f", noRepurchaseCount * 100.0 / dataset.numInstances()));

        // 划分训练集和测试集（80/20）
        dataset.randomize(new java.util.Random(42));
        int trainSize = (int) (dataset.numInstances() * 0.8);
        int testSize = dataset.numInstances() - trainSize;
        Instances trainData = new Instances(dataset, 0, trainSize);
        Instances testData = new Instances(dataset, trainSize, testSize);
        
        log.info("训练集大小: {}", trainSize);
        log.info("测试集大小: {}", testSize);

        // 训练模型
        RandomForest model = new RandomForest();
        model.setNumIterations(30); // 与工具类保持一致
        model.setMaxDepth(12);
        model.setNumFeatures(3);
        model.setSeed(42);
        
        long trainStartTime = System.currentTimeMillis();
        log.info("开始训练随机森林模型...");
        model.buildClassifier(trainData);
        long trainTime = System.currentTimeMillis() - trainStartTime;
        log.info("模型训练完成，耗时: {}ms", trainTime);

        // 评估模型
        Evaluation eval = new Evaluation(trainData);
        eval.evaluateModel(model, testData);
        
        log.info("\n=== 复购预测模型评估结果（测试集） ===");
        log.info("准确率 (Accuracy): {} ({}%)", 
                String.format("%.4f", eval.pctCorrect() / 100.0), 
                String.format("%.2f", eval.pctCorrect()));
        log.info("错误率 (Error Rate): {} ({}%)", 
                String.format("%.4f", eval.pctIncorrect() / 100.0), 
                String.format("%.2f", eval.pctIncorrect()));
        
        // 类别详细指标
        log.info("\n--- 类别详细指标 ---");
        for (int i = 0; i < dataset.classAttribute().numValues(); i++) {
            String className = dataset.classAttribute().value(i);
            log.info("类别 {} ({}):", i, className);
            log.info("  精确率 (Precision): {}", String.format("%.4f", eval.precision(i)));
            log.info("  召回率 (Recall): {}", String.format("%.4f", eval.recall(i)));
            log.info("  F1 分数: {}", String.format("%.4f", eval.fMeasure(i)));
        }
        
        // 混淆矩阵
        log.info("\n--- 混淆矩阵 ---");
        double[][] confusionMatrix = eval.confusionMatrix();
        log.info("实际\\预测    不复购(0)    复购(1)");
        log.info("不复购(0)    {}    {}", 
                String.format("%8.0f", confusionMatrix[0][0]), 
                String.format("%8.0f", confusionMatrix[0][1]));
        log.info("复购(1)      {}    {}", 
                String.format("%8.0f", confusionMatrix[1][0]), 
                String.format("%8.0f", confusionMatrix[1][1]));
        
        // AUC（如果支持）
        try {
            log.info("\nAUC (Area Under ROC): {}", String.format("%.4f", eval.areaUnderROC(1)));
        } catch (Exception e) {
            log.debug("无法计算 AUC: {}", e.getMessage());
        }
        
        // 使用全部数据重新训练（用于实际预测）
        log.info("\n使用全部数据重新训练模型（用于生产预测）...");
        RandomForest finalModel = new RandomForest();
        finalModel.setNumIterations(30);
        finalModel.setMaxDepth(12);
        finalModel.setNumFeatures(3);
        finalModel.setSeed(42);
        finalModel.buildClassifier(dataset);
        
        this.repurchaseModel = finalModel;
        // 仅保存结构用于预测时设置 dataset
        this.repurchaseHeader = new Instances(dataset, 0);
        log.info("模型训练完成，可用于预测");
    }

    @Override
    public com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO getRfmSummaryStats(LocalDate asOfDate) {
        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(asOfDate);
        if (CollectionUtils.isEmpty(all)) {
            return new com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO();
        }

        com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO stats = new com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO();
        stats.setTotalCustomers(all.size());

        long highValueCount = 0;
        long potentialCount = 0;
        long normalCount = 0;
        long churnedCount = 0;
        double totalR = 0, totalF = 0, totalM = 0;

        for (RfmCustomerScoreDTO dto : all) {
            String bucket = toChineseBucket(dto.getSegmentLabel());
            switch (bucket) {
                case "高价值客户":
                    highValueCount++;
                    break;
                case "潜力客户":
                    potentialCount++;
                    break;
                case "流失客户":
                    churnedCount++;
                    break;
                default:
                    normalCount++;
            }

            totalR += dto.getRScore();
            totalF += dto.getFScore();
            totalM += dto.getMScore();
        }

        stats.setHighValueCount(highValueCount);
        stats.setPotentialCount(potentialCount);
        stats.setNormalCount(normalCount);
        stats.setChurnedCount(churnedCount);
        stats.setAvgRScore(round2(totalR / all.size()));
        stats.setAvgFScore(round2(totalF / all.size()));
        stats.setAvgMScore(round2(totalM / all.size()));

        return stats;
    }

    @Override
    public RfmRadarChartDTO getAverageRfmRadar(LocalDate asOfDate) {
        com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO stats = getRfmSummaryStats(asOfDate);

        RfmRadarChartDTO.RadarIndicator r = new RfmRadarChartDTO.RadarIndicator();
        r.setName("最近消费(R)");
        r.setMax(5.0);

        RfmRadarChartDTO.RadarIndicator f = new RfmRadarChartDTO.RadarIndicator();
        f.setName("消费频次(F)");
        f.setMax(5.0);

        RfmRadarChartDTO.RadarIndicator m = new RfmRadarChartDTO.RadarIndicator();
        m.setName("消费金额(M)");
        m.setMax(5.0);

        RfmRadarChartDTO.RadarDataItem item = new RfmRadarChartDTO.RadarDataItem();
        item.setName("RFM 平均得分");
        item.setValue(Arrays.asList(stats.getAvgRScore(), stats.getAvgFScore(), stats.getAvgMScore()));

        RfmRadarChartDTO chartDTO = new RfmRadarChartDTO();
        chartDTO.setIndicators(Arrays.asList(r, f, m));
        chartDTO.setData(Collections.singletonList(item));
        return chartDTO;
    }

    @Override
    public List<com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO> getRepurchaseProbabilityDistribution() {
        try {
            // 检查缓存
            List<com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO> cached = cachedRepurchaseProbDistribution;
            if (cached != null && 
                    (System.currentTimeMillis() - cachedRepurchaseProbDistributionAtMillis) < REPURCHASE_PROB_DISTRIBUTION_CACHE_DURATION_MILLIS) {
                log.info("复购概率分布：使用缓存");
                return cached;
            }
            
            ensureRepurchaseModel();
            if (repurchaseModel == null || repurchaseHeader == null) {
                return Collections.emptyList();
            }

            List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(null);
            if (CollectionUtils.isEmpty(all)) {
                return Collections.emptyList();
            }

            log.info("开始批量预测 {} 个客户的复购概率...", all.size());
            long startTime = System.currentTimeMillis();

            // 批量构造所有客户的 Instance 并预测
            List<Double> probabilities = new ArrayList<>();
            int batchSize = 1000; // 每批处理 1000 个客户
            int processed = 0;

            for (int i = 0; i < all.size(); i += batchSize) {
                int end = Math.min(i + batchSize, all.size());
                List<RfmCustomerScoreDTO> batch = all.subList(i, end);
                
                for (RfmCustomerScoreDTO dto : batch) {
                    try {
                        // 直接构造特征向量，避免重复调用 calculateAllCustomerRfm
                        double[] values = new double[repurchaseHeader.numAttributes()];
                        values[0] = dto.getOrderCount();        // frequency
                        values[1] = dto.getTotalAmount().doubleValue(); // monetary
                        values[2] = dto.getRScore();           // rScore
                        values[3] = dto.getFScore();           // fScore
                        values[4] = dto.getMScore();           // mScore
                        values[5] = Utils.missingValue();      // class 占位

                        DenseInstance instance = new DenseInstance(1.0, values);
                        instance.setDataset(repurchaseHeader);

                        double[] dist = repurchaseModel.distributionForInstance(instance);
                        int yesIndex = repurchaseHeader.classAttribute().indexOfValue("1");
                        if (yesIndex >= 0 && yesIndex < dist.length) {
                            probabilities.add(round2(dist[yesIndex]));
                        } else {
                            probabilities.add(0.0);
                        }
                        processed++;
                    } catch (Exception e) {
                        log.debug("预测客户 {} 失败: {}", dto.getCustomerId(), e.getMessage());
                        probabilities.add(0.0);
                        processed++;
                    }
                }
                
                // 每处理一批输出进度
                if (processed % 5000 == 0) {
                    log.info("已处理 {} / {} 个客户", processed, all.size());
                }
            }

            long predictTime = System.currentTimeMillis() - startTime;
            log.info("批量预测完成，共处理 {} 个客户，耗时: {}ms", processed, predictTime);

            if (probabilities.isEmpty()) {
                return Collections.emptyList();
            }

            // 将概率分为 10 个区间：0.0-0.1, 0.1-0.2, ..., 0.9-1.0
            // 注意：概率范围是 [0, 1]，所以：
            // - prob = 0.0 时，index = 0 (0.0-0.1)
            // - prob = 0.1 时，index = 1 (0.1-0.2)
            // - prob = 1.0 时，index = 9 (0.9-1.0)
            long[] counts = new long[10];
            for (double prob : probabilities) {
                // 确保概率在 [0, 1] 范围内
                prob = Math.max(0.0, Math.min(1.0, prob));
                // 计算区间索引：0.0-0.1 -> 0, 0.1-0.2 -> 1, ..., 0.9-1.0 -> 9
                int index;
                if (prob >= 1.0) {
                    index = 9; // 1.0 归入最后一个区间
                } else {
                    index = (int) (prob * 10);
                    index = Math.min(index, 9); // 确保不超过 9
                }
                counts[index]++;
            }

            List<com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO> result = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO dto = 
                        new com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO();
                dto.setProbabilityRange(String.format("%.1f-%.1f", i * 0.1, (i + 1) * 0.1));
                dto.setCount(counts[i]);
                result.add(dto);
            }

            // 输出分布统计（带区间标签）
            StringBuilder distInfo = new StringBuilder("\n复购概率分布详情:\n");
            for (int i = 0; i < 10; i++) {
                String range = String.format("%.1f-%.1f", i * 0.1, (i + 1) * 0.1);
                distInfo.append(String.format("  %s: %d 个客户 (%.2f%%)\n", 
                        range, counts[i], counts[i] * 100.0 / probabilities.size()));
            }
            log.info(distInfo.toString());
            
            // 统计摘要
            long lowProbCount = counts[0] + counts[1] + counts[2] + counts[3] + counts[4]; // 0.0-0.5
            long midProbCount = counts[5] + counts[6] + counts[7]; // 0.5-0.8
            long highProbCount = counts[8] + counts[9]; // 0.8-1.0
            log.info("复购概率分布摘要: 低概率(0-0.5)={} ({}%), 中概率(0.5-0.8)={} ({}%), 高概率(0.8-1.0)={} ({}%)",
                    lowProbCount, String.format("%.2f", lowProbCount * 100.0 / probabilities.size()),
                    midProbCount, String.format("%.2f", midProbCount * 100.0 / probabilities.size()),
                    highProbCount, String.format("%.2f", highProbCount * 100.0 / probabilities.size()));

            // 更新缓存
            cachedRepurchaseProbDistribution = result;
            cachedRepurchaseProbDistributionAtMillis = System.currentTimeMillis();

            return result;
        } catch (Exception e) {
            log.error("计算复购概率分布失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<com.example.ecommerce.dto.analysis.RfmScatterDataDTO> getRfmScatterData(int sampleSize) {
        try {
            List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(null);
            if (CollectionUtils.isEmpty(all)) {
                return Collections.emptyList();
            }

            List<RfmCustomerScoreDTO> dataToUse;
            if (sampleSize > 0 && sampleSize < all.size()) {
                // 采样：随机选择指定数量的客户
                List<RfmCustomerScoreDTO> shuffled = new ArrayList<>(all);
                Collections.shuffle(shuffled, new java.util.Random(42));
                dataToUse = shuffled.subList(0, sampleSize);
                log.info("RFM 散点图：从 {} 个客户中采样 {} 个", all.size(), sampleSize);
            } else {
                dataToUse = all;
                log.info("RFM 散点图：使用全部 {} 个客户数据", all.size());
            }

            List<com.example.ecommerce.dto.analysis.RfmScatterDataDTO> result = dataToUse.stream()
                    .map(dto -> {
                        com.example.ecommerce.dto.analysis.RfmScatterDataDTO scatter = 
                                new com.example.ecommerce.dto.analysis.RfmScatterDataDTO();
                        scatter.setCustomerId(dto.getCustomerId());
                        scatter.setRScore(dto.getRScore());
                        scatter.setFScore(dto.getFScore());
                        scatter.setMScore(dto.getMScore());
                        scatter.setTotalAmount(dto.getTotalAmount());
                        scatter.setSegmentLabel(dto.getSegmentLabel());
                        return scatter;
                    })
                    .collect(Collectors.toList());
            
            log.info("RFM 散点图：返回 {} 条数据", result.size());
            if (!result.isEmpty()) {
                log.info("RFM 散点图：前5条数据示例 - R:{}, F:{}, M:{}", 
                        result.get(0).getRScore(), result.get(0).getFScore(), result.get(0).getMScore());
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取 RFM 散点图数据失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<com.example.ecommerce.dto.analysis.RfmScoreDistributionDTO> getRfmScoreDistribution(LocalDate asOfDate) {
        List<RfmCustomerScoreDTO> all = calculateAllCustomerRfm(asOfDate);
        if (CollectionUtils.isEmpty(all)) {
            return Collections.emptyList();
        }

        // 按 RFM 总分区间分组（1.0-2.0, 2.0-3.0, 3.0-4.0, 4.0-5.0）
        // 注意：RFM 总分 = 0.3*r + 0.3*f + 0.4*m，其中 r/f/m 都是 1-5，所以总分范围是 1.0-5.0
        Map<String, Long> map = new HashMap<>();
        for (RfmCustomerScoreDTO dto : all) {
            double score = dto.getRfmScore();
            String range;
            if (score < 2.0) {
                range = "1.0-2.0";
            } else if (score < 3.0) {
                range = "2.0-3.0";
            } else if (score < 4.0) {
                range = "3.0-4.0";
            } else {
                range = "4.0-5.0";
            }
            map.merge(range, 1L, Long::sum);
        }

        List<com.example.ecommerce.dto.analysis.RfmScoreDistributionDTO> result = new ArrayList<>();
        String[] ranges = {"1.0-2.0", "2.0-3.0", "3.0-4.0", "4.0-5.0"};
        for (String range : ranges) {
            com.example.ecommerce.dto.analysis.RfmScoreDistributionDTO dto = new com.example.ecommerce.dto.analysis.RfmScoreDistributionDTO();
            dto.setScoreRange(range);
            dto.setCount(map.getOrDefault(range, 0L));
            result.add(dto);
        }

        return result;
    }
}


