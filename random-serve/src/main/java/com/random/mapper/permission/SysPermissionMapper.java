package com.random.mapper.permission;

import com.random.pojo.dto.PermissionPageRequest;
import com.random.pojo.entity.SysPermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 权限数据访问接口。
 */
@Mapper
public interface SysPermissionMapper {

    /**
     * 查询指定用户的权限编码集合。
     *
     * @param userId 用户 ID
     * @return 该用户拥有的权限编码列表
     */
    @Select("select distinct p.permission_code from sys_permission p " +
            "inner join sys_role_permission rp on p.id = rp.permission_id " +
            "inner join sys_user_role ur on rp.role_id = ur.role_id " +
            "where ur.user_id = #{userId} and p.status = 1 and p.deleted = 0")
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 查询指定用户的菜单权限集合。
     *
     * @param userId 用户 ID
     * @return 该用户拥有的菜单权限列表
     */
    @Select("select distinct p.* from sys_permission p " +
            "inner join sys_role_permission rp on p.id = rp.permission_id " +
            "inner join sys_user_role ur on rp.role_id = ur.role_id " +
            "where ur.user_id = #{userId} and p.status = 1 and p.deleted = 0 and p.permission_type = 'menu' " +
            "order by p.sort, p.id")
    List<SysPermission> getMenusByUserId(Long userId);

    /**
     * 分页查询权限（配合 PageHelper 使用）。
     *
     * @param request 分页条件
     * @return 权限实体列表
     */
    @Select("<script>" +
            "select * from sys_permission where deleted = 0 " +
            "<if test='permissionName != null and permissionName != \"\"'>" +
            "and permission_name like concat('%', #{permissionName}, '%') " +
            "</if>" +
            "order by sort, id" +
            "</script>")
    List<SysPermission> pageQuery(PermissionPageRequest request);

    /**
     * 查询所有未删除的权限。
     *
     * @return 权限实体列表
     */
    @Select("select * from sys_permission where deleted = 0 order by sort, id")
    List<SysPermission> listAll();

    /**
     * 根据权限 ID 查询权限。
     *
     * @param id 权限 ID
     * @return 匹配的权限，未找到时返回 null
     */
    @Select("select * from sys_permission where id = #{id} and deleted = 0")
    SysPermission getById(Long id);

    /**
     * 根据权限编码查询权限。
     *
     * @param permissionCode 权限编码
     * @return 匹配的权限，未找到时返回 null
     */
    @Select("select * from sys_permission where permission_code = #{permissionCode} and deleted = 0")
    SysPermission getByCode(String permissionCode);

    /**
     * 新增权限。
     *
     * @param permission 权限实体
     * @return 受影响的行数
     */
    @Insert("insert into sys_permission (parent_id, permission_name, permission_code, permission_type, " +
            "path, component, api_url, method, sort, status, deleted, create_time) " +
            "values (#{parentId}, #{permissionName}, #{permissionCode}, #{permissionType}, " +
            "#{path}, #{component}, #{apiUrl}, #{method}, #{sort}, #{status}, #{deleted}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysPermission permission);

    /**
     * 更新权限非空字段。
     *
     * @param permission 权限实体，仅需设置 id 及待更新字段
     * @return 受影响的行数
     */
    @Update("<script>" +
            "update sys_permission " +
            "<set>" +
            "<if test='parentId != null'>parent_id = #{parentId},</if>" +
            "<if test='permissionName != null'>permission_name = #{permissionName},</if>" +
            "<if test='permissionCode != null'>permission_code = #{permissionCode},</if>" +
            "<if test='permissionType != null'>permission_type = #{permissionType},</if>" +
            "<if test='path != null'>path = #{path},</if>" +
            "<if test='component != null'>component = #{component},</if>" +
            "<if test='apiUrl != null'>api_url = #{apiUrl},</if>" +
            "<if test='method != null'>method = #{method},</if>" +
            "<if test='sort != null'>sort = #{sort},</if>" +
            "<if test='status != null'>status = #{status},</if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    int update(SysPermission permission);

    /**
     * 逻辑删除权限。
     *
     * @param id 权限 ID
     * @return 受影响的行数
     */
    @Update("update sys_permission set deleted = 1 where id = #{id}")
    int logicalDelete(@Param("id") Long id);

}
