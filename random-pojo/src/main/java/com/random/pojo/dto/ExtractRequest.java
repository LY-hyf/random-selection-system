package com.random.pojo.dto;

import lombok.Data;

/**
 * 随机抽取专家请求。
 */
@Data
public class ExtractRequest {

    /** 申报类型 */
    private String applyType;

    /** 技术类型 */
    private String technicalType;

    /** 级别 */
    private String level;

    /** 抽取数量，默认 5 */
    private Integer count = 5;
}
