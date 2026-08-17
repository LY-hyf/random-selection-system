package com.random.pojo.vo.permission;

import lombok.Data;

import java.util.List;

/**
 * 权限树视图对象。
 */
@Data
public class PermissionTreeVO {

    /** 权限 ID */
    private Long id;

    /** 父权限 ID */
    private Long parentId;

    /** 权限名称 */
    private String permissionName;

    /** 权限编码 */
    private String permissionCode;

    /** 权限类型（menu/button/api） */
    private String permissionType;

    /** 前端路由路径 */
    private String path;

    /** 前端组件 */
    private String component;

    /** 排序号 */
    private Integer sort;

    /** 子权限集合 */
    private List<PermissionTreeVO> children;
}
