package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.*;

import java.util.List;

/**
 * 用户行为路径分析服务
 */
public interface UserBehaviorAnalysisService {

    /**
     * 用户活跃时间热力图（周几 × 几点）
     */
    List<UserActiveTimeHeatmapDTO> getUserActiveTimeHeatmap();

    /**
     * 订单状态漏斗图（下单 → 批准 → 发货 → 交付）
     */
    List<OrderFunnelDTO> getOrderFunnel();

    /**
     * 用户购买行为桑基图（首次购买 → 复购流转）
     */
    UserBehaviorSankeyDTO getUserBehaviorSankey();
}

