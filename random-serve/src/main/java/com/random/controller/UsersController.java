package com.random.controller;

import com.random.annotation.Log;
import com.random.pojo.dto.AssignRoleRequest;
import com.random.pojo.dto.PageRequest;
import com.random.pojo.dto.ResetPasswordRequest;
import com.random.pojo.dto.UserCreateRequest;
import com.random.pojo.dto.UserUpdateRequest;
import com.random.pojo.vo.UserVO;
import com.random.result.PageResult;
import com.random.result.Result;
import com.random.service.UsersService;
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
 * 用户管理控制器。
 *
 * <p>提供用户的增删改查、密码重置与角色分配等管理接口。</p>
 */
@RestController
@RequestMapping("/users")
@Slf4j
public class UsersController {

    /** 用户管理服务 */
    @Autowired
    private UsersService usersService;

    /**
     * 分页查询用户列表。
     *
     * @param request 分页及筛选条件
     * @return 分页结果，包含用户列表与总记录数
     */
    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(PageRequest request) {
        log.info("分页查询用户列表, username: {}, nickname: {}, status: {}, pageNum: {}, pageSize: {}",
                request.getUsername(), request.getNickname(), request.getStatus(),
                request.getPageNum(), request.getPageSize());
        return Result.success(usersService.page(request));
    }

    /**
     * 新增用户。
     *
     * @param request 新增用户请求体
     * @return 新增结果
     */
    @Log(module = "用户管理", operation = "新增用户")
    @PreAuthorize("hasAuthority('user:add')")
    @PostMapping
    public Result add(@Valid @RequestBody UserCreateRequest request) {
        log.info("新增用户, username: {}, nickname: {}, phone: {}", request.getUsername(),
                request.getNickname(), request.getPhone());
        usersService.add(request);
        return Result.success();
    }

    /**
     * 编辑用户。
     *
     * @param id      用户 ID
     * @param request 编辑用户请求体
     * @return 编辑结果
     */
    @Log(module = "用户管理", operation = "编辑用户")
    @PreAuthorize("hasAuthority('user:edit')")
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        log.info("编辑用户, id: {}, nickname: {}, phone: {}, status: {}", id,
                request.getNickname(), request.getPhone(), request.getStatus());
        usersService.update(id, request);
        return Result.success();
    }

    /**
     * 删除用户（逻辑删除）。
     *
     * @param id 用户 ID
     * @return 删除结果
     */
    @Log(module = "用户管理", operation = "删除用户")
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("删除用户（逻辑删除）, id: {}", id);
        usersService.delete(id);
        return Result.success();
    }

    /**
     * 重置用户密码。
     *
     * @param id      用户 ID
     * @param request 重置密码请求体
     * @return 重置结果
     */
    @Log(module = "用户管理", operation = "重置密码")
    @PreAuthorize("hasAuthority('user:resetPwd')")
    @PutMapping("/{id}/reset-password")
    public Result resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        log.info("重置用户密码, id: {}", id);
        usersService.resetPassword(id, request);
        return Result.success();
    }

    /**
     * 获取用户已分配的角色。
     *
     * @param id 用户 ID
     * @return 用户已分配的角色 ID 集合
     */
    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        log.info("查询用户已分配角色, id: {}", id);
        return Result.success(usersService.getUserRoles(id));
    }

    /**
     * 为用户分配角色。
     *
     * @param id      用户 ID
     * @param request 分配角色请求体
     * @return 分配结果
     */
    @Log(module = "用户管理", operation = "分配角色")
    @PreAuthorize("hasAuthority('user:assignRole')")
    @PutMapping("/{id}/roles")
    public Result assignRoles(@PathVariable Long id, @RequestBody AssignRoleRequest request) {
        log.info("为用户分配角色, id: {}, roleIds: {}", id, request.getRoleIds());
        usersService.assignRoles(id, request);
        return Result.success();
    }
}
