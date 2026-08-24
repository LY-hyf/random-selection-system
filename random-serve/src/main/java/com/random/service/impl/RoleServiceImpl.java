package com.random.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.random.constant.StatusConstant;
import com.random.exception.LoginFailedException;
import com.random.mapper.role.SysRoleMapper;
import com.random.mapper.role.SysRolePermissionMapper;
import com.random.pojo.dto.AssignPermissionRequest;
import com.random.pojo.dto.RoleCreateRequest;
import com.random.pojo.dto.RolePageRequest;
import com.random.pojo.dto.RoleUpdateRequest;
import com.random.pojo.entity.SysRole;
import com.random.pojo.vo.RoleVO;
import com.random.result.PageResult;
import com.random.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色管理服务实现类。
 *
 * <p>实现角色的分页查询、增删改及权限分配等业务逻辑。</p>
 */
@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    /** 角色数据访问接口 */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 角色-权限关联数据访问接口 */
    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    /**
     * 分页查询角色。
     *
     * @param request 分页条件
     * @return 角色分页结果
     */
    @Override
    public PageResult<SysRole> page(RolePageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysRole> list = sysRoleMapper.pageQuery(request);
        PageInfo<SysRole> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 查询所有启用角色（精简字段）。
     *
     * @return 所有启用角色列表
     */
    @Override
    public List<RoleVO> all() {
        List<SysRole> list = sysRoleMapper.getAll();
        List<RoleVO> result = new ArrayList<>();
        if (list != null) {
            for (SysRole role : list) {
                RoleVO vo = new RoleVO();
                vo.setId(role.getId());
                vo.setRoleName(role.getRoleName());
                vo.setRoleCode(role.getRoleCode());
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 新增角色。
     *
     * <p>校验角色编码唯一性，并填充默认的状态与删除标记。</p>
     *
     * @param request 新增角色请求
     */
    @Override
    public void add(RoleCreateRequest request) {
        SysRole existing = sysRoleMapper.getByRoleCode(request.getRoleCode());
        if (existing != null) {
            throw new LoginFailedException("角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() == null ? StatusConstant.ENABLE : request.getStatus());
        role.setDeleted(0);
        role.setCreateTime(LocalDateTime.now());

        sysRoleMapper.insert(role);
        log.info("新增角色成功，id: {}, roleCode: {}", role.getId(), role.getRoleCode());
    }

    /**
     * 编辑角色。
     *
     * @param id      角色 ID
     * @param request 编辑角色请求
     */
    @Override
    public void update(Long id, RoleUpdateRequest request) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        sysRoleMapper.update(role);
        log.info("编辑角色成功，id: {}", id);
    }

    /**
     * 逻辑删除角色。
     *
     * @param id 角色 ID
     */
    @Override
    public void delete(Long id) {
        sysRoleMapper.logicalDelete(id);
        log.info("删除角色成功，id: {}", id);
    }

    /**
     * 获取角色已分配的权限 ID 集合。
     *
     * @param id 角色 ID
     * @return 角色已分配的权限 ID 集合
     */
    @Override
    public List<Long> getPermissions(Long id) {
        List<Long> permissionIds = sysRolePermissionMapper.getPermissionIdsByRoleId(id);
        return permissionIds != null ? permissionIds : new ArrayList<>();
    }

    /**
     * 为角色分配权限。
     *
     * <p>先删除角色原有权限关联，再批量写入新的权限关联，整个过程在同一事务内保证原子性。</p>
     *
     * @param id      角色 ID
     * @param request 分配权限请求
     */
    @Override
    @Transactional
    public void assignPermissions(Long id, AssignPermissionRequest request) {
        sysRolePermissionMapper.deleteByRoleId(id);
        List<Long> permissionIds = request.getPermissionIds();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            sysRolePermissionMapper.insertBatch(id, permissionIds);
        log.info("分配权限成功，roleId: {}, permissionIds: {}", id, request.getPermissionIds());
        }
    }

}
