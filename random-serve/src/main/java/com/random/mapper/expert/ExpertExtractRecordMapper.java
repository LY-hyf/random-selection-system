package com.random.mapper.expert;

import com.random.pojo.dto.ExtractHistoryPageRequest;
import com.random.pojo.entity.ExpertExtractRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 专家抽取记录数据访问接口。
 */
@Mapper
public interface ExpertExtractRecordMapper {

    /**
     * 分页查询抽取历史（配合 PageHelper 使用）。
     *
     * @param request 分页及筛选条件
     * @return 抽取历史记录列表
     */
    @Select("<script>" +
            "select r.id, r.batch_no as batchNo, e.name as expertName, r.user_id as userId, " +
            "u.username as username, r.apply_type as applyType, d1.dict_value as applyTypeLabel, " +
            "r.technical_type as technicalType, d2.dict_value as technicalTypeLabel, " +
            "r.level, d3.dict_value as levelLabel, " +
            "date_format(r.extract_time, '%Y-%m-%d %H:%i:%s') as extractTime " +
            "from expert_extract_record r " +
            "left join expert_info e on r.expert_id = e.id " +
            "left join sys_user u on r.user_id = u.id " +
            "left join sys_dict d1 on d1.dict_type = 'apply_type' and d1.dict_code = r.apply_type " +
            "left join sys_dict d2 on d2.dict_type = 'technical_type' and d2.dict_code = r.technical_type " +
            "left join sys_dict d3 on d3.dict_type = 'level' and d3.dict_code = r.level " +
            "<where>" +
            "<if test='batchNo != null and batchNo != \"\"'>and r.batch_no like concat('%', #{batchNo}, '%')</if>" +
            "<if test='startTime != null and startTime != \"\"'>and r.extract_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null and endTime != \"\"'>and r.extract_time &lt;= #{endTime}</if>" +
            "</where>" +
            "order by r.extract_time desc" +
            "</script>")
    List<Map<String, Object>> pageQuery(ExtractHistoryPageRequest request);

    /**
     * 批量查询专家的最近抽取时间。
     *
     * @param expertIds 专家 ID 集合
     * @return 每行包含 expertId 与 lastExtractTime 两个键
     */
    @Select("<script>" +
            "select expert_id as expertId, max(extract_time) as lastExtractTime " +
            "from expert_extract_record where expert_id in " +
            "<foreach collection='expertIds' item='eid' open='(' separator=',' close=')'>#{eid}</foreach>" +
            " group by expert_id" +
            "</script>")
    List<Map<String, Object>> getLastExtractTimeByExpertIds(@Param("expertIds") List<Long> expertIds);

    /**
     * 新增抽取记录。
     *
     * @param record 抽取记录实体
     * @return 受影响的行数
     */
    @Insert("insert into expert_extract_record (batch_no, expert_id, user_id, apply_type, technical_type, level, extract_time, create_time) " +
            "values (#{batchNo}, #{expertId}, #{userId}, #{applyType}, #{technicalType}, #{level}, #{extractTime}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExpertExtractRecord record);

    /**
     * 查询指定批次的专家信息（用于导出）。
     *
     * @param batchNo 抽取批次号
     * @return 该批次下的专家信息列表
     */
    @Select("select e.name as name, e.apply_type as applyType, e.technical_type as technicalType, " +
            "e.level as level, e.phone as phone, e.company as company " +
            "from expert_extract_record r join expert_info e on r.expert_id = e.id " +
            "where r.batch_no = #{batchNo}")
    List<Map<String, Object>> getExpertsByBatchNo(@Param("batchNo") String batchNo);



}
