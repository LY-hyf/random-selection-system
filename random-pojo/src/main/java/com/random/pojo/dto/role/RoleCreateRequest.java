package com.random.pojo.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 新增角色请求。
 */
@Data
public class RoleCreateRequest {

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色编码 */
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /** 角色描述 */
    private String description;

    /** 状态：1 启用，0 禁用 */
    private Integer status;
}
