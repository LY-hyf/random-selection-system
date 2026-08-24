package com.random.service;

import com.random.pojo.dto.AssignRoleRequest;
import com.random.pojo.dto.PageRequest;
import com.random.pojo.dto.ResetPasswordRequest;
import com.random.pojo.dto.UserCreateRequest;
import com.random.pojo.dto.UserUpdateRequest;
import com.random.pojo.vo.UserVO;
import com.random.result.PageResult;

import java.util.List;

/**
 * 用户管理服务接口。
 *
 * <p>定义用户的增删改查、密码重置与角色分配等业务能力。</p>
 */
public interface UsersService {

    /**
     * 分页查询用户。
     *
     * @param request 分页及筛选条件
     * @return 分页结果，包含用户列表与总记录数
     */
    PageResult<UserVO> page(PageRequest request);

    /**
     * 新增用户。
     *
     * @param request 新增用户请求
     */
    void add(UserCreateRequest request);

    /**
     * 编辑用户。
     *
     * @param id      用户 ID
     * @param request 编辑用户请求
     */
    void update(Long id, UserUpdateRequest request);

    /**
     * 逻辑删除用户。
     *
     * @param id 用户 ID
     */
    void delete(Long id);

    /**
     * 重置用户密码。
     *
     * @param id      用户 ID
     * @param request 重置密码请求
     */
    void resetPassword(Long id, ResetPasswordRequest request);

    /**
     * 获取用户已分配的角色 ID 集合。
     *
     * @param id 用户 ID
     * @return 用户已分配的角色 ID 集合
     */
    List<Long> getUserRoles(Long id);

    /**
     * 为用户分配角色。
     *
     * @param id      用户 ID
     * @param request 分配角色请求
     */
    void assignRoles(Long id, AssignRoleRequest request);

}
