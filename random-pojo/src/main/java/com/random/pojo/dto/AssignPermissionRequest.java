package com.random.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 为角色分配权限请求。
 */
@Data
public class AssignPermissionRequest {

    /** 待分配的权限 ID 集合 */
    private List<Long> permissionIds;
}
