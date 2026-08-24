package com.random.mapper.role;

import com.random.pojo.dto.RolePageRequest;
import com.random.pojo.entity.SysRole;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 角色数据访问接口。
 */
@Mapper
public interface SysRoleMapper {

    /**
     * 查询指定用户的角色编码集合。
     *
     * @param userId 用户 ID
     * @return 该用户拥有的角色编码列表
     */
    @Select("select r.role_code from sys_role r " +
            "inner join sys_user_role ur on r.id = ur.role_id " +
            "where ur.user_id = #{userId} and r.status = 1 and r.deleted = 0")
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 批量查询多个用户的角色名称。
     *
     * @param userIds 用户 ID 集合
     * @return 每行包含 userId 与 roleName 两个键
     */
    @Select("<script>" +
            "select ur.user_id as userId, r.role_name as roleName " +
            "from sys_user_role ur join sys_role r on ur.role_id = r.id " +
            "where r.deleted = 0 and ur.user_id in " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>" +
            "</script>")
    List<Map<String, Object>> getRoleNamesByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 分页查询角色（配合 PageHelper 使用，不写 LIMIT）。
     *
     * @param request 分页条件
     * @return 角色实体列表
     */
    @Select("<script>" +
            "select * from sys_role where deleted = 0 " +
            "<if test='roleName != null and roleName != \"\"'>" +
            "and role_name like concat('%', #{roleName}, '%') " +
            "</if>" +
            "order by id" +
            "</script>")
    List<SysRole> pageQuery(RolePageRequest request);

    /**
     * 查询所有启用角色。
     *
     * @return 启用角色列表
     */
    @Select("select * from sys_role where deleted = 0 and status = 1 order by id")
    List<SysRole> getAll();

    /**
     * 根据角色 ID 查询角色。
     *
     * @param id 角色 ID
     * @return 匹配的角色，未找到时返回 null
     */
    @Select("select * from sys_role where id = #{id} and deleted = 0")
    SysRole getById(Long id);

    /**
     * 根据角色编码查询角色。
     *
     * @param roleCode 角色编码
     * @return 匹配的角色，未找到时返回 null
     */
    @Select("select * from sys_role where role_code = #{roleCode} and deleted = 0")
    SysRole getByRoleCode(String roleCode);

    /**
     * 新增角色。
     *
     * @param role 角色实体
     * @return 受影响的行数
     */
    @Insert("insert into sys_role (role_name, role_code, description, status, deleted, create_time) " +
            "values (#{roleName}, #{roleCode}, #{description}, #{status}, #{deleted}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysRole role);

    /**
     * 更新角色非空字段。
     *
     * @param role 角色实体，仅需设置 id 及待更新字段
     * @return 受影响的行数
     */
    @Update("<script>" +
            "update sys_role " +
            "<set>" +
            "<if test='roleName != null'>role_name = #{roleName},</if>" +
            "<if test='description != null'>description = #{description},</if>" +
            "<if test='status != null'>status = #{status},</if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    int update(SysRole role);

    /**
     * 逻辑删除角色。
     *
     * @param id 角色 ID
     * @return 受影响的行数
     */
    @Update("update sys_role set deleted = 1 where id = #{id}")
    int logicalDelete(@Param("id") Long id);

}
