package com.random.pojo.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 为用户分配角色请求。
 */
@Data
public class AssignRoleRequest {

    /** 待分配的角色 ID 集合 */
    private List<Long> roleIds;
}
