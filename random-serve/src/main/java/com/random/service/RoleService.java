package com.random.service;

import com.random.pojo.dto.AssignPermissionRequest;
import com.random.pojo.dto.RoleCreateRequest;
import com.random.pojo.dto.RolePageRequest;
import com.random.pojo.dto.RoleUpdateRequest;
import com.random.pojo.entity.SysRole;
import com.random.pojo.vo.RoleVO;
import com.random.result.PageResult;

import java.util.List;

/**
 * 角色管理服务接口。
 *
 * <p>定义角色的分页查询、增删改及权限分配等业务能力。</p>
 */
public interface RoleService {

    /**
     * 分页查询角色。
     *
     * @param request 分页条件
     * @return 角色分页结果
     */
    PageResult<SysRole> page(RolePageRequest request);

    /**
     * 查询所有启用角色（精简字段）。
     *
     * @return 所有启用角色列表
     */
    List<RoleVO> all();

    /**
     * 新增角色。
     *
     * @param request 新增角色请求
     */
    void add(RoleCreateRequest request);

    /**
     * 编辑角色。
     *
     * @param id      角色 ID
     * @param request 编辑角色请求
     */
    void update(Long id, RoleUpdateRequest request);

    /**
     * 逻辑删除角色。
     *
     * @param id 角色 ID
     */
    void delete(Long id);

    /**
     * 获取角色已分配的权限 ID 集合。
     *
     * @param id 角色 ID
     * @return 角色已分配的权限 ID 集合
     */
    List<Long> getPermissions(Long id);

    /**
     * 为角色分配权限。
     *
     * @param id      角色 ID
     * @param request 分配权限请求
     */
    void assignPermissions(Long id, AssignPermissionRequest request);

}
