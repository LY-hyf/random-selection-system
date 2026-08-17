package com.random.pojo.dto.expert;

import lombok.Data;

/**
 * 专家分页查询请求。
 */
@Data
public class ExpertPageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 申报类型 */
    private String applyType;

    /** 技术类型 */
    private String technicalType;

    /** 级别 */
    private String level;

    /** 姓名（模糊匹配） */
    private String name;
}
