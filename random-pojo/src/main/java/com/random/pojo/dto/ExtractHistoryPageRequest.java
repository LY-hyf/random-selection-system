package com.random.pojo.dto;

import lombok.Data;

/**
 * 抽取历史分页查询请求。
 */
@Data
public class ExtractHistoryPageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 抽取批次号 */
    private String batchNo;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;
}
