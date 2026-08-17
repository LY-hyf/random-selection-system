package com.random.mapper.role;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联数据访问接口。
 */
@Mapper
public interface SysRolePermissionMapper {

    /**
     * 查询指定角色已分配的权限 ID 集合。
     *
     * @param roleId 角色 ID
     * @return 该角色已分配的权限 ID 集合
     */
    @Select("select permission_id from sys_role_permission where role_id = #{roleId}")
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 删除指定角色的全部权限关联。
     *
     * @param roleId 角色 ID
     * @return 受影响的行数
     */
    @Delete("delete from sys_role_permission where role_id = #{roleId}")
    int deleteByRoleId(Long roleId);

    /**
     * 批量新增角色-权限关联。
     *
     * @param roleId        角色 ID
     * @param permissionIds 待关联的权限 ID 集合
     * @return 受影响的行数
     */
    @Insert("<script>" +
            "insert into sys_role_permission (role_id, permission_id, create_time) values " +
            "<foreach collection='permissionIds' item='permId' separator=','>" +
            "(#{roleId}, #{permId}, now())" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

}
