package com.random.pojo.dto.permission;

import lombok.Data;

/**
 * 权限分页查询请求。
 */
@Data
public class PermissionPageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 权限名称（模糊匹配） */
    private String permissionName;
}
