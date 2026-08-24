package com.random.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类。
 *
 * <p>对应数据库表 sys_operation_log，记录用户的关键操作行为。</p>
 */
@Data
public class SysOperationLog {

    /** 主键 ID */
    private Long id;

    /** 操作用户 ID */
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 操作所属模块 */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求地址 */
    private String requestUrl;

    /** 请求方法 */
    private String requestMethod;

    /** 客户端 IP */
    private String ip;

    /** 操作结果：1 成功，0 失败 */
    private Integer result;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
