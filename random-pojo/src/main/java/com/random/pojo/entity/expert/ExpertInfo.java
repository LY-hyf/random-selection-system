package com.random.pojo.entity.expert;

import lombok.Data;

import javax.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专家信息实体类。
 *
 * <p>对应数据库表 expert_info，描述参与随机抽取的专家基本信息。</p>
 */
@Data
public class ExpertInfo {

    /** 主键 ID */
    private Long id;

    /** 专家姓名 */
    private String name;

    /** 出生日期 */
    private LocalDate birthday;

    /** 学历 */
    private String education;

    /** 工作单位 */
    private String company;

    /** 申请类型 */
    private String applyType;

    /** 技术类型 */
    private String technicalType;

    /** 专家等级 */
    private String level;

    /** 联系电话 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 逻辑删除标记：0 未删除，1 已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录更新时间 */
    private LocalDateTime updateTime;
}
