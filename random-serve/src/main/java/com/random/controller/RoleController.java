package com.random.controller;

import com.random.annotation.Log;
import com.random.pojo.dto.AssignPermissionRequest;
import com.random.pojo.dto.RoleCreateRequest;
import com.random.pojo.dto.RolePageRequest;
import com.random.pojo.dto.RoleUpdateRequest;
import com.random.pojo.entity.SysRole;
import com.random.pojo.vo.RoleVO;
import com.random.result.PageResult;
import com.random.result.Result;
import com.random.service.RoleService;
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

import javax.validation.Valid;
import java.util.List;

/**
 * 角色管理控制器。
 *
 * <p>提供角色的分页查询、增删改及权限分配接口。</p>
 */
@RestController
@RequestMapping("/roles")
@Slf4j
public class RoleController {

    /** 角色管理服务 */
    @Autowired
    private RoleService roleService;

    /**
     * 分页查询角色列表。
     *
     * @param request 分页条件
     * @return 角色分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(RolePageRequest request) {
        log.debug("分页查询角色列表, pageNum: {}, pageSize: {}", request.getPageNum(), request.getPageSize());
        return Result.success(roleService.page(request));
    }

    /**
     * 查询所有角色（下拉选择）。
     *
     * @return 所有角色列表
     */
    @GetMapping("/all")
    public Result<List<RoleVO>> all() {
        log.debug("查询所有角色");
        return Result.success(roleService.all());
    }

    /**
     * 新增角色。
     *
     * @param request 新增角色请求
     * @return 新增结果
     */
    @Log(module = "角色管理", operation = "新增角色")
    @PreAuthorize("hasAuthority('role:add')")
    @PostMapping
    public Result add(@Valid @RequestBody RoleCreateRequest request) {
        log.info("新增角色, roleName: {}", request.getRoleName());
        roleService.add(request);
        return Result.success();
    }

    /**
     * 编辑角色。
     *
     * @param id      角色 ID
     * @param request 编辑角色请求
     * @return 编辑结果
     */
    @Log(module = "角色管理", operation = "编辑角色")
    @PreAuthorize("hasAuthority('role:edit')")
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        log.info("编辑角色, id: {}", id);
        roleService.update(id, request);
        return Result.success();
    }

    /**
     * 删除角色（逻辑删除）。
     *
     * @param id 角色 ID
     * @return 删除结果
     */
    @Log(module = "角色管理", operation = "删除角色")
    @PreAuthorize("hasAuthority('role:delete')")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("删除角色, id: {}", id);
        roleService.delete(id);
        return Result.success();
    }

    /**
     * 获取角色已分配的权限。
     *
     * @param id 角色 ID
     * @return 角色已分配的权限 ID 集合
     */
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        log.debug("获取角色已分配权限, id: {}", id);
        return Result.success(roleService.getPermissions(id));
    }

    /**
     * 为角色分配权限。
     *
     * @param id      角色 ID
     * @param request 分配权限请求
     * @return 分配结果
     */
    @Log(module = "角色管理", operation = "分配权限")
    @PreAuthorize("hasAuthority('role:assignPerm')")
    @PutMapping("/{id}/permissions")
    public Result assignPermissions(@PathVariable Long id, @RequestBody AssignPermissionRequest request) {
        log.info("为角色分配权限, id: {}, permissionIds: {}", id, request.getPermissionIds());
        roleService.assignPermissions(id, request);
        return Result.success();
    }

}
