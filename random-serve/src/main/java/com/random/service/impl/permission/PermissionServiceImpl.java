package com.random.service.impl.permission;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.random.constant.StatusConstant;
import com.random.context.BaseContext;
import com.random.exception.LoginFailedException;
import com.random.mapper.permission.SysPermissionMapper;
import com.random.pojo.dto.permission.PermissionPageRequest;
import com.random.pojo.entity.permission.SysPermission;
import com.random.pojo.vo.permission.PermissionTreeVO;
import com.random.result.PageResult;
import com.random.service.permission.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理服务实现类。
 *
 * <p>实现权限的分页查询、树构建、增删改及菜单查询等业务逻辑。</p>
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    /** 权限数据访问接口 */
    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    /**
     * 分页查询权限。
     *
     * @param request 分页条件
     * @return 权限分页结果
     */
    @Override
    public PageResult<SysPermission> page(PermissionPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysPermission> list = sysPermissionMapper.pageQuery(request);
        PageInfo<SysPermission> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 获取权限树。
     *
     * @return 权限树结构
     */
    @Override
    public List<PermissionTreeVO> tree() {
        List<SysPermission> permissions = sysPermissionMapper.listAll();
        return buildTree(permissions);
    }

    /**
     * 新增权限。
     *
     * <p>校验权限编码唯一性，并填充默认的父级、排序、状态等字段。</p>
     *
     * @param permission 权限实体
     */
    @Override
    public void add(SysPermission permission) {
        SysPermission existing = sysPermissionMapper.getByCode(permission.getPermissionCode());
        if (existing != null) {
            throw new LoginFailedException("权限编码已存在");
        }
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getSort() == null) {
            permission.setSort(0);
        }
        permission.setStatus(permission.getStatus() == null ? StatusConstant.ENABLE : permission.getStatus());
        permission.setDeleted(0);
        permission.setCreateTime(LocalDateTime.now());
        sysPermissionMapper.insert(permission);
        log.info("新增权限成功，id: {}, code: {}", permission.getId(), permission.getPermissionCode());
    }

    /**
     * 编辑权限。
     *
     * @param id         权限 ID
     * @param permission 权限实体
     */
    @Override
    public void update(Long id, SysPermission permission) {
        permission.setId(id);
        sysPermissionMapper.update(permission);
        log.info("编辑权限成功，id: {}", id);
    }

    /**
     * 逻辑删除权限。
     *
     * @param id 权限 ID
     */
    @Override
    public void delete(Long id) {
        sysPermissionMapper.logicalDelete(id);
        log.info("删除权限成功，id: {}", id);
    }

    /**
     * 获取当前用户的权限菜单树。
     *
     * @return 当前用户可见的权限菜单树，未登录时返回空列表
     */
    @Override
    public List<PermissionTreeVO> menu() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return new ArrayList<>();
        }
        List<SysPermission> permissions = sysPermissionMapper.getMenusByUserId(userId);
        return buildTree(permissions);
    }

    /**
     * 将平铺的权限列表构建为树形结构。
     *
     * @param permissions 平铺的权限列表
     * @return 以根节点为顶层的权限树
     */
    private List<PermissionTreeVO> buildTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, PermissionTreeVO> map = new HashMap<>();
        for (SysPermission p : permissions) {
            PermissionTreeVO vo = new PermissionTreeVO();
            vo.setId(p.getId());
            vo.setParentId(p.getParentId());
            vo.setPermissionName(p.getPermissionName());
            vo.setPermissionCode(p.getPermissionCode());
            vo.setPermissionType(p.getPermissionType());
            vo.setPath(p.getPath());
            vo.setComponent(p.getComponent());
            vo.setSort(p.getSort());
            vo.setChildren(new ArrayList<>());
            map.put(p.getId(), vo);
        }

        List<PermissionTreeVO> roots = new ArrayList<>();
        for (PermissionTreeVO vo : map.values()) {
            if (vo.getParentId() != null && vo.getParentId() != 0 && map.containsKey(vo.getParentId())) {
                map.get(vo.getParentId()).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

}
