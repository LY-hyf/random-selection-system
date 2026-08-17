package com.random.mapper.user;

import com.random.pojo.dto.user.PageRequest;
import com.random.pojo.entity.user.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统用户数据访问接口。
 *
 * <p>提供系统用户相关的查询与写入能力，部分分页查询在 SysUserMapper.xml 中定义。</p>
 */
@Mapper
public interface SysUserMapper {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 匹配的用户，未找到时返回 null
     */
    @Select("select * from sys_user where username = #{username} and deleted = 0")
    SysUser getByUsername(String username);

    /**
     * 根据用户 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 匹配的用户，未找到时返回 null
     */
    @Select("select * from sys_user where id = #{id} and deleted = 0")
    SysUser getById(Long id);

    /**
     * 新增用户。
     *
     * @param user 用户实体
     * @return 受影响的行数
     */
    @Insert("insert into sys_user (username, password, nickname, phone, status, deleted, create_time) " +
            "values (#{username}, #{password}, #{nickname}, #{phone}, #{status}, #{deleted}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    /**
     * 逻辑删除用户。
     *
     * @param id 用户 ID
     * @return 受影响的行数
     */
    @Update("update sys_user set deleted = 1 where id = #{id}")
    int logicalDelete(@Param("id") Long id);

    /**
     * 更新用户密码。
     *
     * @param id       用户 ID
     * @param password 加密后的新密码
     * @return 受影响的行数
     */
    @Update("update sys_user set password = #{password} where id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 分页查询用户（见 SysUserMapper.xml）。
     *
     * @param request 分页及筛选条件
     * @return 用户实体列表
     */
    List<SysUser> pageQuery(PageRequest request);

    /**
     * 统计满足条件的用户数（见 SysUserMapper.xml）。
     *
     * @param request 分页及筛选条件
     * @return 满足条件的用户总数
     */
    long count(PageRequest request);

    /**
     * 更新用户信息（见 SysUserMapper.xml）。
     *
     * <p>根据传入的非空字段动态更新昵称、手机号与状态。</p>
     *
     * @param user 用户实体，仅需设置 id 及待更新字段
     * @return 受影响的行数
     */
    int update(SysUser user);

}
