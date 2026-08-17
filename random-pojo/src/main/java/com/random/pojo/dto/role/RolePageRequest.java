package com.random.pojo.dto.role;

import lombok.Data;

/**
 * 角色分页查询请求。
 */
@Data
public class RolePageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 角色名称（模糊匹配） */
    private String roleName;
}
