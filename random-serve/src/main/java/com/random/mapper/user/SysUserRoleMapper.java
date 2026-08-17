package com.random.mapper.user;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联数据访问接口。
 *
 * <p>提供用户与角色关联关系的查询与维护能力。</p>
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 查询指定用户已分配的角色 ID 集合。
     *
     * @param userId 用户 ID
     * @return 该用户已分配的角色 ID 集合
     */
    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 删除指定用户的全部角色关联。
     *
     * @param userId 用户 ID
     * @return 受影响的行数
     */
    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(Long userId);

    /**
     * 批量新增用户-角色关联。
     *
     * @param userId  用户 ID
     * @param roleIds 待关联的角色 ID 集合
     * @return 受影响的行数
     */
    @Insert("<script>" +
            "insert into sys_user_role (user_id, role_id, create_time) values " +
            "<foreach collection='roleIds' item='roleId' separator=','>" +
            "(#{userId}, #{roleId}, now())" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

}
