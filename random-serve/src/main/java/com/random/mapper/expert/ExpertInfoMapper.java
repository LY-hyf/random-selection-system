package com.random.mapper.expert;

import com.random.pojo.dto.ExpertPageRequest;
import com.random.pojo.entity.ExpertInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 专家信息数据访问接口。
 */
@Mapper
public interface ExpertInfoMapper {

    /**
     * 分页查询专家（配合 PageHelper 使用）。
     *
     * @param request 分页及筛选条件
     * @return 专家实体列表
     */
    @Select("<script>" +
            "select * from expert_info where deleted = 0 " +
            "<if test='applyType != null and applyType != \"\"'>and apply_type = #{applyType} </if>" +
            "<if test='technicalType != null and technicalType != \"\"'>and technical_type = #{technicalType} </if>" +
            "<if test='level != null and level != \"\"'>and level = #{level} </if>" +
            "<if test='name != null and name != \"\"'>and name like concat('%', #{name}, '%') </if>" +
            "order by id" +
            "</script>")
    List<ExpertInfo> pageQuery(ExpertPageRequest request);

    /**
     * 根据专家 ID 查询专家。
     *
     * @param id 专家 ID
     * @return 匹配的专家，未找到时返回 null
     */
    @Select("select * from expert_info where id = #{id} and deleted = 0")
    ExpertInfo getById(Long id);

    /**
     * 新增专家。
     *
     * @param expert 专家实体
     * @return 受影响的行数
     */
    @Insert("insert into expert_info (name, birthday, education, company, apply_type, technical_type, " +
            "level, phone, status, deleted, create_time) " +
            "values (#{name}, #{birthday}, #{education}, #{company}, #{applyType}, #{technicalType}, " +
            "#{level}, #{phone}, #{status}, #{deleted}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExpertInfo expert);

    /**
     * 批量新增专家（用于大文件流式导入）。
     *
     * @param experts 专家实体列表
     * @return 受影响的行数
     */
    @Insert("<script>" +
            "insert into expert_info (name, birthday, education, company, apply_type, technical_type, " +
            "level, phone, status, deleted, create_time) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.name}, #{e.birthday}, #{e.education}, #{e.company}, #{e.applyType}, #{e.technicalType}, " +
            "#{e.level}, #{e.phone}, #{e.status}, #{e.deleted}, #{e.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<ExpertInfo> experts);

    /**
     * 更新专家非空字段。
     *
     * @param expert 专家实体，仅需设置 id 及待更新字段
     * @return 受影响的行数
     */
    @Update("<script>" +
            "update expert_info " +
            "<set>" +
            "<if test='name != null'>name = #{name},</if>" +
            "<if test='birthday != null'>birthday = #{birthday},</if>" +
            "<if test='education != null'>education = #{education},</if>" +
            "<if test='company != null'>company = #{company},</if>" +
            "<if test='applyType != null'>apply_type = #{applyType},</if>" +
            "<if test='technicalType != null'>technical_type = #{technicalType},</if>" +
            "<if test='level != null'>level = #{level},</if>" +
            "<if test='phone != null'>phone = #{phone},</if>" +
            "<if test='status != null'>status = #{status},</if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    int update(ExpertInfo expert);

    /**
     * 逻辑删除专家。
     *
     * @param id 专家 ID
     * @return 受影响的行数
     */
    @Update("update expert_info set deleted = 1 where id = #{id}")
    int logicalDelete(@Param("id") Long id);

    /**
     * 查询所有申报类型（去重）。
     *
     * @return 申报类型编码列表
     */
    @Select("select distinct apply_type from expert_info where deleted = 0 and apply_type is not null")
    List<String> getDistinctApplyTypes();

    /**
     * 查询所有技术类型（去重）。
     *
     * @return 技术类型编码列表
     */
    @Select("select distinct technical_type from expert_info where deleted = 0 and technical_type is not null")
    List<String> getDistinctTechnicalTypes();

    /**
     * 查询所有级别（去重）。
     *
     * @return 级别编码列表
     */
    @Select("select distinct level from expert_info where deleted = 0 and level is not null")
    List<String> getDistinctLevels();

    /**
     * 查询可抽取的专家（排除近 30 天内已抽取的）。
     *
     * <p>申报类型、技术类型、级别为「同时满足」的 AND 交集关系，未填写的条件不参与过滤。</p>
     */
    @Select("<script>" +
            "select e.* from expert_info e where e.deleted = 0 and e.status = 1 " +
            "and not exists (select 1 from expert_extract_record r where r.expert_id = e.id " +
            "and r.extract_time >= date_sub(now(), interval 30 day)) " +
            "<if test=\"applyType != null and applyType != ''\">and e.apply_type = #{applyType} </if>" +
            "<if test=\"technicalType != null and technicalType != ''\">and e.technical_type = #{technicalType} </if>" +
            "<if test=\"level != null and level != ''\">and e.level = #{level} </if>" +
            "</script>")
    List<ExpertInfo> getExtractableExperts(@Param("applyType") String applyType,
                                           @Param("technicalType") String technicalType,
                                           @Param("level") String level);

    /**
     * 获取所有查询条件的组合
     *
     * @author hyf
     * @since 2026/8/25
     */
    @Select("select DISTINCT apply_type, technical_type, level FROM expert_info WHERE status=1 AND deleted=0")
    List<Map<String, String>> getDistinctCombinations();

    /**
     * 获取在特定查询条件下30天内未被抽取的专家Id
     *
     * @param applyType
     * @param techType
     * @param level
     * @return 专家Id
     * @author hyf
     * @since 2026/8/25
     */
    @Select("SELECT id FROM expert_info e WHERE e.status=1 AND e.deleted=0 " +
            "AND e.apply_type=#{applyType} AND e.technical_type=#{techType} AND e.level=#{level} " +
            "AND NOT EXISTS (SELECT 1 FROM expert_extract_record r WHERE r.expert_id = e.id " +
            "AND r.extract_time > DATE_SUB(NOW(), INTERVAL 30 DAY))")
    List<Long> getExtractableExpertIds(@Param("applyType") String applyType,
                                       @Param("techType") String techType,
                                       @Param("level") String level);

    /**
     *根据专家id批量获取专家信息
     * @author hyf
     * @since 2026/8/25
     */
    @Select("<script>" +
            "SELECT id, name, birthday, education, company, " +
            "apply_type, technical_type, level, phone, " +
            "status, deleted, create_time, update_time " +
            "FROM expert_info " +
            "WHERE id IN " +
            "<foreach collection='ids' item='item' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
   List<ExpertInfo> selectBatchIds(@Param("ids") List<Long> ids);
}
