package com.random.pojo.vo.expert;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专家视图对象。
 */
@Data
public class ExpertVO {

    /** 专家 ID */
    private Long id;

    /** 姓名 */
    private String name;

    /** 出生日期 */
    private LocalDate birthday;

    /** 学历 */
    private String education;

    /** 学历（中文标签） */
    private String educationLabel;

    /** 工作单位 */
    private String company;

    /** 申报类型（编码） */
    private String applyType;

    /** 申报类型（中文标签） */
    private String applyTypeLabel;

    /** 技术类型 */
    private String technicalType;

    /** 技术类型（中文标签） */
    private String technicalTypeLabel;

    /** 级别 */
    private String level;

    /** 级别（中文标签） */
    private String levelLabel;

    /** 联系方式 */
    private String phone;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 抽取状态 */
    private String extractStatus;

    /** 最近抽取时间 */
    private LocalDateTime lastExtractTime;
}
