package com.random.mapper.dict;

import com.random.pojo.entity.SysDict;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 数据字典数据访问接口。
 */
@Mapper
public interface SysDictMapper {

    /**
     * 按字典类型查询启用的字典项。
     *
     * @param dictType 字典类型
     * @return 该类型下启用的字典项列表
     */
    @Select("select * from sys_dict where dict_type = #{dictType} and status = 1 order by sort")
    List<SysDict> getByType(String dictType);

    /**
     * 查询所有字典项。
     *
     * @return 所有启用的字典项列表
     */
    @Select("select * from sys_dict where status = 1 order by dict_type, sort")
    List<SysDict> getAll();

    /**
     * 新增字典项。
     *
     * @param dict 字典实体
     * @return 受影响的行数
     */
    @Insert("insert into sys_dict (dict_type, dict_code, dict_value, sort, status, create_time) " +
            "values (#{dictType}, #{dictCode}, #{dictValue}, #{sort}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysDict dict);

    /**
     * 更新字典项。
     *
     * @param dict 字典实体，仅需设置 id 及待更新字段
     * @return 受影响的行数
     */
    @Update("<script>" +
            "update sys_dict " +
            "<set>" +
            "<if test='dictValue != null'>dict_value = #{dictValue},</if>" +
            "<if test='sort != null'>sort = #{sort},</if>" +
            "<if test='status != null'>status = #{status},</if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    int update(SysDict dict);

    /**
     * 删除字典项。
     *
     * @param id 字典 ID
     * @return 受影响的行数
     */
    @Delete("delete from sys_dict where id = #{id}")
    int deleteById(@Param("id") Long id);

}
