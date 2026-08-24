package com.random.pojo.vo;

import lombok.Data;

/**
 * 角色精简视图对象（用于下拉选择）。
 */
@Data
public class RoleVO {

    /** 角色 ID */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;
}
