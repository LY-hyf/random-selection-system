package com.random.pojo.vo.expert;

import lombok.Data;

/**
 * 抽取结果中的专家精简视图对象。
 */
@Data
public class ExtractExpertVO {

    /** 专家 ID */
    private Long id;

    /** 姓名 */
    private String name;

    /** 申报类型 */
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

    /** 工作单位 */
    private String company;
}
