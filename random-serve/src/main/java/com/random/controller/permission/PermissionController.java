package com.random.controller.permission;

import com.random.annotation.Log;
import com.random.pojo.dto.permission.PermissionPageRequest;
import com.random.pojo.entity.permission.SysPermission;
import com.random.pojo.vo.permission.PermissionTreeVO;
import com.random.result.PageResult;
import com.random.result.Result;
import com.random.service.permission.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器。
 *
 * <p>提供权限的分页查询、权限树、菜单及增删改接口。</p>
 */
@RestController
@RequestMapping("/permissions")
@Slf4j
public class PermissionController {

    /** 权限管理服务 */
    @Autowired
    private PermissionService permissionService;

    /**
     * 分页查询权限列表。
     *
     * @param request 分页条件
     * @return 权限分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<SysPermission>> page(PermissionPageRequest request) {
        log.debug("分页查询权限列表, pageNum: {}, pageSize: {}", request.getPageNum(), request.getPageSize());
        return Result.success(permissionService.page(request));
    }

    /**
     * 获取权限树。
     *
     * @return 权限树结构
     */
    @GetMapping("/tree")
    public Result<List<PermissionTreeVO>> tree() {
        log.debug("获取权限树");
        return Result.success(permissionService.tree());
    }

    /**
     * 获取当前用户权限菜单。
     *
     * @return 当前用户可见的权限菜单树
     */
    @GetMapping("/menu")
    public Result<List<PermissionTreeVO>> menu() {
        log.debug("获取当前用户权限菜单");
        return Result.success(permissionService.menu());
    }

    /**
     * 新增权限。
     *
     * @param permission 权限实体
     * @return 新增结果
     */
    @Log(module = "权限管理", operation = "新增权限")
    @PreAuthorize("hasAuthority('permission:add')")
    @PostMapping
    public Result add(@RequestBody SysPermission permission) {
        log.info("新增权限, permissionName: {}", permission.getPermissionName());
        permissionService.add(permission);
        return Result.success();
    }

    /**
     * 编辑权限。
     *
     * @param id         权限 ID
     * @param permission 权限实体
     * @return 编辑结果
     */
    @Log(module = "权限管理", operation = "编辑权限")
    @PreAuthorize("hasAuthority('permission:edit')")
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody SysPermission permission) {
        log.info("编辑权限, id: {}", id);
        permissionService.update(id, permission);
        return Result.success();
    }

    /**
     * 删除权限（逻辑删除）。
     *
     * @param id 权限 ID
     * @return 删除结果
     */
    @Log(module = "权限管理", operation = "删除权限")
    @PreAuthorize("hasAuthority('permission:delete')")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("删除权限, id: {}", id);
        permissionService.delete(id);
        return Result.success();
    }

}
