package com.random.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类。
 *
 * <p>对应数据库表 sys_role，描述系统中的角色信息。</p>
 */
@Data
public class SysRole {

    /** 主键 ID */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;

    /** 角色描述 */
    private String description;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 逻辑删除标记：0 未删除，1 已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录更新时间 */
    private LocalDateTime updateTime;
}
