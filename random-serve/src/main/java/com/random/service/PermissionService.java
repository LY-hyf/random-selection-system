package com.random.service;

import com.random.pojo.dto.PermissionPageRequest;
import com.random.pojo.entity.SysPermission;
import com.random.pojo.vo.PermissionTreeVO;
import com.random.result.PageResult;

import java.util.List;

/**
 * 权限管理服务接口。
 *
 * <p>定义权限的查询、增删改及菜单树构建等业务能力。</p>
 */
public interface PermissionService {

    /**
     * 分页查询权限。
     *
     * @param request 分页条件
     * @return 权限分页结果
     */
    PageResult<SysPermission> page(PermissionPageRequest request);

    /**
     * 获取权限树。
     *
     * @return 权限树结构
     */
    List<PermissionTreeVO> tree();

    /**
     * 新增权限。
     *
     * @param permission 权限实体
     */
    void add(SysPermission permission);

    /**
     * 编辑权限。
     *
     * @param id         权限 ID
     * @param permission 权限实体
     */
    void update(Long id, SysPermission permission);

    /**
     * 逻辑删除权限。
     *
     * @param id 权限 ID
     */
    void delete(Long id);

    /**
     * 获取当前用户的权限菜单树。
     *
     * @return 当前用户可见的权限菜单树
     */
    List<PermissionTreeVO> menu();

}
