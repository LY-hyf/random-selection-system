package com.random.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专家抽取记录实体类。
 *
 * <p>对应数据库表 expert_extract_record，记录每次专家抽取的批次与明细。</p>
 */
@Data
public class ExpertExtractRecord {

    /** 主键 ID */
    private Long id;

    /** 抽取批次号 */
    private String batchNo;

    /** 被抽取专家 ID */
    private Long expertId;

    /** 抽取操作人 ID */
    private Long userId;

    /** 申请类型 */
    private String applyType;

    /** 技术类型 */
    private String technicalType;

    /** 专家等级 */
    private String level;

    /** 抽取时间 */
    private LocalDateTime extractTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
