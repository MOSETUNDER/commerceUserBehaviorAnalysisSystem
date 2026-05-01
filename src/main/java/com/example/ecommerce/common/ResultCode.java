package com.example.ecommerce.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 * 
 * @author system
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    
    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),
    
    /**
     * 失败
     */
    ERROR(500, "操作失败"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    
    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),
    
    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),
    
    /**
     * 数据已存在
     */
    DATA_EXISTS(409, "数据已存在"),
    
    /**
     * 系统异常
     */
    SYSTEM_ERROR(500, "系统异常");
    
    /**
     * 响应码
     */
    private final Integer code;
    
    /**
     * 响应消息
     */
    private final String message;
}

