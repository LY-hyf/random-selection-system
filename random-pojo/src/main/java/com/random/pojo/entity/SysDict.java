package com.random.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典实体类。
 *
 * <p>对应数据库表 sys_dict，用于维护系统中的字典类型与键值。</p>
 */
@Data
public class SysDict {

    /** 主键 ID */
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private String dictCode;

    /** 字典值 */
    private String dictValue;

    /** 排序号 */
    private Integer sort;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
