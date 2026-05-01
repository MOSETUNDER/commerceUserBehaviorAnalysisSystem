package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.util.List;

/**
 * 用户行为桑基图 DTO
 */
@Data
public class UserBehaviorSankeyDTO {
    /**
     * 节点列表（每个节点代表一个状态）
     */
    private List<SankeyNode> nodes;

    /**
     * 连接列表（每个连接代表状态流转）
     */
    private List<SankeyLink> links;

    @Data
    public static class SankeyNode {
        /**
         * 节点名称
         */
        private String name;
    }

    @Data
    public static class SankeyLink {
        /**
         * 源节点索引
         */
        private int source;

        /**
         * 目标节点索引
         */
        private int target;

        /**
         * 流转数量（用户数）
         */
        private long value;
    }
}

