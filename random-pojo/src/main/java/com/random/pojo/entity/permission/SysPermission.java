package com.random.pojo.entity.permission;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体类。
 *
 * <p>对应数据库表 sys_permission，描述系统中的权限（菜单/按钮/接口）信息。</p>
 */
@Data
public class SysPermission {

    /** 主键 ID */
    private Long id;

    /** 父权限 ID */
    private Long parentId;

    /** 权限名称 */
    private String permissionName;

    /** 权限编码 */
    private String permissionCode;

    /** 权限类型（如菜单、按钮、接口） */
    private String permissionType;

    /** 前端路由路径 */
    private String path;

    /** 前端组件 */
    private String component;

    /** 接口地址 */
    private String apiUrl;

    /** 请求方法 */
    private String method;

    /** 排序号 */
    private Integer sort;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 逻辑删除标记：0 未删除，1 已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录更新时间 */
    private LocalDateTime updateTime;
}
