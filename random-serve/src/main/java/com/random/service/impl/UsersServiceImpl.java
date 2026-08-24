package com.random.service.impl;

import com.random.constant.StatusConstant;
import com.random.exception.LoginFailedException;
import com.random.mapper.role.SysRoleMapper;
import com.random.mapper.user.SysUserMapper;
import com.random.mapper.user.SysUserRoleMapper;
import com.random.pojo.dto.AssignRoleRequest;
import com.random.pojo.dto.PageRequest;
import com.random.pojo.dto.ResetPasswordRequest;
import com.random.pojo.dto.UserCreateRequest;
import com.random.pojo.dto.UserUpdateRequest;
import com.random.pojo.entity.SysUser;
import com.random.pojo.vo.UserVO;
import com.random.result.PageResult;
import com.random.service.UsersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现类。
 *
 * <p>实现用户的分页查询、新增、编辑、删除、密码重置与角色分配等业务逻辑。</p>
 */
@Service
@Slf4j
public class UsersServiceImpl implements UsersService {

    /** 系统用户数据访问接口 */
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 用户-角色关联数据访问接口 */
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /** 角色数据访问接口 */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 密码编码器 */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户。
     *
     * <p>先统计总记录数，再查询当前页数据，并批量填充每个用户的角色名称。</p>
     *
     * @param request 分页及筛选条件
     * @return 分页结果，包含用户列表与总记录数
     */
    @Override
    public PageResult<UserVO> page(PageRequest request) {
        long total = sysUserMapper.count(request);
        List<SysUser> users = sysUserMapper.pageQuery(request);

        List<UserVO> records = new ArrayList<>();
        if (users != null && !users.isEmpty()) {
            List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
            Map<Long, List<String>> roleMap = buildRoleNameMap(userIds);

            for (SysUser user : users) {
                UserVO vo = new UserVO();
                vo.setId(user.getId());
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setPhone(user.getPhone());
                vo.setStatus(user.getStatus());
                vo.setCreateTime(user.getCreateTime());
                vo.setRoleNames(roleMap.getOrDefault(user.getId(), new ArrayList<>()));
                records.add(vo);
            }
        }
        return new PageResult<>(total, records);
    }

    /**
     * 新增用户。
     *
     * <p>校验用户名是否已存在，不存在则加密密码后写入用户表。</p>
     *
     * @param request 新增用户请求
     */
    @Override
    public void add(UserCreateRequest request) {
        SysUser existing = sysUserMapper.getByUsername(request.getUsername());
        if (existing != null) {
            throw new LoginFailedException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() == null ? StatusConstant.ENABLE : request.getStatus());
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());

        sysUserMapper.insert(user);
        log.info("新增用户成功，id: {}, username: {}", user.getId(), user.getUsername());
    }

    /**
     * 编辑用户。
     *
     * @param id      用户 ID
     * @param request 编辑用户请求
     */
    @Override
    public void update(Long id, UserUpdateRequest request) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        sysUserMapper.update(user);
        log.info("编辑用户成功，id: {}", id);
    }

    /**
     * 逻辑删除用户。
     *
     * @param id 用户 ID
     */
    @Override
    public void delete(Long id) {
        sysUserMapper.logicalDelete(id);
        log.info("删除用户成功，id: {}", id);
    }

    /**
     * 重置用户密码。
     *
     * @param id      用户 ID
     * @param request 重置密码请求
     */
    @Override
    public void resetPassword(Long id, ResetPasswordRequest request) {
        sysUserMapper.updatePassword(id, passwordEncoder.encode(request.getNewPassword()));
        log.info("重置密码成功，id: {}", id);
    }

    /**
     * 获取用户已分配的角色 ID 集合。
     *
     * @param id 用户 ID
     * @return 用户已分配的角色 ID 集合
     */
    @Override
    public List<Long> getUserRoles(Long id) {
        List<Long> roleIds = sysUserRoleMapper.getRoleIdsByUserId(id);
        return roleIds != null ? roleIds : new ArrayList<>();
    }

    /**
     * 为用户分配角色。
     *
     * <p>对传入的RoleIds[]进行null过滤，先删除用户原有角色关联，再批量写入新的角色关联，整个过程在同一事务内保证原子性。</p>
     *
     * @param id      用户 ID
     * @param request 分配角色请求
     */
    @Override
    @Transactional
    public void assignRoles(Long id, AssignRoleRequest request) {
        // 1. 防御：如果 request 或 roleIds 为 null，视为空列表
        List<Long> originalRoleIds = request.getRoleIds();
        if (originalRoleIds == null) {
            originalRoleIds = Collections.emptyList();
        }

        // 2. 过滤掉 null 并去重（可选去重）
        List<Long> validRoleIds = originalRoleIds.stream()
                .filter(Objects::nonNull)
                .distinct()  // 可选：去重，避免插入重复关联
                .collect(Collectors.toList());

        // 3. 先删除旧关联
        sysUserRoleMapper.deleteByUserId(id);

        // 4. 如果有有效角色，才执行批量插入
        if (!validRoleIds.isEmpty()) {
            sysUserRoleMapper.insertBatch(id, validRoleIds);
        }

        // 5. 日志记录有效数据，便于追踪
        log.info("分配角色成功，userId: {}, 原始请求: {}, 实际分配: {}",
                id, originalRoleIds, validRoleIds);
    }

    /**
     * 构建用户 ID 到角色名称列表的映射。
     *
     * @param userIds 用户 ID 集合
     * @return 以用户 ID 为键、角色名称列表为值的映射
     */
    private Map<Long, List<String>> buildRoleNameMap(List<Long> userIds) {
        Map<Long, List<String>> roleMap = new HashMap<>();
        List<Map<String, Object>> roleRows = sysRoleMapper.getRoleNamesByUserIds(userIds);
        if (roleRows == null) {
            return roleMap;
        }
        for (Map<String, Object> row : roleRows) {
            Long userId = ((Number) row.get("userId")).longValue();
            String roleName = (String) row.get("roleName");
            roleMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleName);
        }
        return roleMap;
    }

}
