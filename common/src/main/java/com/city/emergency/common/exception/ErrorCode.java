package com.city.emergency.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    
    LOCATION_NOT_FOUND(1001, "定位信息不存在"),
    LOCATION_UPDATE_FAILED(1002, "定位更新失败"),
    FUSION_FAILED(1003, "定位数据融合失败"),
    
    SCHEDULING_FAILED(2001, "调度算法执行失败"),
    NO_AVAILABLE_RESOURCE(2002, "没有可用的资源"),
    SCHEDULING_TIMEOUT(2003, "调度超时"),
    
    RESOURCE_NOT_AVAILABLE(3001, "资源不可用"),
    RESOURCE_ALREADY_IN_USE(3002, "资源已被占用"),
    INVALID_RESOURCE_TYPE(3003, "无效的资源类型"),
    
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态错误"),
    ORDER_CREATE_FAILED(4003, "订单创建失败"),
    
    STAFF_NOT_FOUND(5001, "服务人员不存在"),
    STAFF_SKILL_MISMATCH(5002, "服务人员技能不匹配"),
    STAFF_UNAVAILABLE(5003, "服务人员不可用");

    private final Integer code;
    private final String message;
}
