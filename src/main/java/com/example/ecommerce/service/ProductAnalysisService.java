package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.*;

import java.util.List;

/**
 * 商品 / 品类相关分析服务
 */
public interface ProductAnalysisService {

    /**
     * Top N 品类销售额柱状图
     */
    List<CategorySalesDTO> getTopCategorySales(int topN);

    /**
     * 品类复购率热力图（品类 × 用户分层）
     */
    List<CategoryRepurchaseHeatmapDTO> getCategoryRepurchaseHeatmap();

    /**
     * Top N 商品销售条形图
     */
    List<ProductSalesDTO> getTopProductSales(int topN);

    /**
     * 品类各州销售地图
     */
    List<CategoryStateSalesDTO> getCategoryStateSales();
}


