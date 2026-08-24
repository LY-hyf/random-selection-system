package com.random.pojo.dto;

import lombok.Data;

/**
 * 操作日志分页查询请求。
 */
@Data
public class LogPageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 用户名（模糊匹配） */
    private String username;

    /** 模块 */
    private String module;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;
}
