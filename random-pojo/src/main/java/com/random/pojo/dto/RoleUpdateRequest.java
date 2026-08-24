package com.random.pojo.dto;

import lombok.Data;

/**
 * 编辑角色请求。
 */
@Data
public class RoleUpdateRequest {

    /** 角色名称 */
    private String roleName;

    /** 角色描述 */
    private String description;

    /** 状态：1 启用，0 禁用 */
    private Integer status;
}
