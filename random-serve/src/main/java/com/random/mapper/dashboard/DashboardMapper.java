package com.random.mapper.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘数据访问接口。
 *
 * <p>提供统计看板所需数据的数据库查询能力。</p>
 */
@Mapper
public interface DashboardMapper {

    /**
     * 统计专家总数。
     *
     * @return 未删除的专家总数
     */
    @Select("select count(*) from expert_info where deleted = 0")
    Long countTotalExperts();

    /**
     * 统计本月新增专家数。
     *
     * @return 本月新增的专家数
     */
    @Select("select count(*) from expert_info where deleted = 0 and create_time >= date_format(date_sub(now(), interval 1 month), '%Y-%m-01')")
    Long countMonthNewExperts();

    /**
     * 统计本月抽取批次总数。
     *
     * @return 本月抽取的批次数量
     */
    @Select("select count(distinct batch_no) from expert_extract_record where extract_time >= date_format(date_sub(now(), interval 1 month), '%Y-%m-01')")
    Long countMonthExtract();

    /**
     * 查询近期抽取趋势。
     *
     * @return 按日期统计的抽取批次数量列表
     */
    @Select("select date_format(extract_time, '%m-%d') as date, count(distinct batch_no) as count " +
            "from expert_extract_record where extract_time >= date_sub(curdate(), interval 7 day) " +
            "group by date_format(extract_time, '%m-%d') order by date")
    List<Map<String, Object>> selectRecentExtractTrend();

    /**
     * 查询技术类型分布（字典转中文标签）。
     *
     * @return 各技术类型的专家数量列表
     */
    @Select("select d.dict_value as name, count(*) as value " +
            "from expert_info e join sys_dict d on d.dict_type = 'technical_type' and d.dict_code = e.technical_type " +
            "where e.deleted = 0 and e.status = 1 group by d.dict_value")
    List<Map<String, Object>> selectTechnicalDistribution();

    /**
     * 查询专家等级分布（字典转中文标签）。
     *
     * @return 各等级的专家数量列表
     */
    @Select("select d.dict_value as level, count(*) as count " +
            "from expert_info e join sys_dict d on d.dict_type = 'level' and d.dict_code = e.level " +
            "where e.deleted = 0 and e.status = 1 group by d.dict_value")
    List<Map<String, Object>> selectLevelDistribution();

    /**
     * 查询申请类型分布（字典转中文标签）。
     *
     * @return 各申请类型的专家数量列表
     */
    @Select("select d.dict_value as name, count(*) as value " +
            "from expert_info e join sys_dict d on d.dict_type = 'apply_type' and d.dict_code = e.apply_type " +
            "where e.deleted = 0 and e.status = 1 group by d.dict_value")
    List<Map<String, Object>> selectApplyTypeDistribution();

    /**
     * 查询近 7 天抽取趋势。
     *
     * @return 近 7 天按日期统计的抽取批次数量列表
     */
    @Select("select date_format(r.extract_time, '%Y-%m-%d') as date, count(distinct r.batch_no) as count " +
            "from expert_extract_record r where r.extract_time >= date_sub(curdate(), interval 7 day) " +
            "group by date_format(r.extract_time, '%Y-%m-%d') order by date")
    List<Map<String, Object>> selectExtractTrend7Days();

    /**
     * 查询最新抽取记录。
     *
     * @param limit 返回的记录条数
     * @return 最新抽取记录列表
     */
    @Select("select r.batch_no as batchNo, e.name as expertName, u.username as operator, " +
            "date_format(r.extract_time, '%Y-%m-%d %H:%i:%s') as extractTime " +
            "from expert_extract_record r " +
            "left join expert_info e on r.expert_id = e.id " +
            "left join sys_user u on r.user_id = u.id " +
            "order by r.extract_time desc limit #{limit}")
    List<Map<String, Object>> selectLatestExtracts(int limit);

    /**
     * 查询最新抽取批次（按批次号分组，返回批次号、操作人、抽取时间、专家数量）。
     *
     * @param limit 返回的批次条数
     * @return 最新抽取批次列表
     */
    @Select("select r.batch_no as batchNo, u.username as operator, " +
            "date_format(max(r.extract_time), '%Y-%m-%d %H:%i:%s') as extractTime, count(*) as expertCount " +
            "from expert_extract_record r left join sys_user u on r.user_id = u.id " +
            "group by r.batch_no, u.username " +
            "order by r.batch_no desc limit #{limit}")
    List<Map<String, Object>> selectLatestBatches(int limit);

    /**
     * 批量查询多个批次的专家明细。
     *
     * @param batchNos 批次号集合
     * @return 各批次的专家明细列表
     */
    @Select("<script>" +
            "select r.batch_no as batchNo, e.name as expertName, e.apply_type as applyType, d1.dict_value as applyTypeLabel, " +
            "e.technical_type as technicalType, d2.dict_value as technicalTypeLabel, " +
            "e.level as level, d3.dict_value as levelLabel, e.phone as phone, e.company as company " +
            "from expert_extract_record r " +
            "join expert_info e on r.expert_id = e.id " +
            "left join sys_dict d1 on d1.dict_type = 'apply_type' and d1.dict_code = e.apply_type " +
            "left join sys_dict d2 on d2.dict_type = 'technical_type' and d2.dict_code = e.technical_type " +
            "left join sys_dict d3 on d3.dict_type = 'level' and d3.dict_code = e.level " +
            "where r.batch_no in " +
            "<foreach collection='batchNos' item='bn' open='(' separator=',' close=')'>#{bn}</foreach>" +
            "</script>")
    List<Map<String, Object>> getExpertsByBatchNos(@Param("batchNos") List<String> batchNos);

    /**
     * 按批次号集合查询抽取记录明细（用于导出，含操作人、抽取时间）。
     *
     * @param batchNos 批次号集合
     * @return 抽取记录明细列表
     */
    @Select("<script>" +
            "select r.batch_no as batchNo, e.name as expertName, u.username as operator, " +
            "date_format(r.extract_time, '%Y-%m-%d %H:%i:%s') as extractTime " +
            "from expert_extract_record r " +
            "left join expert_info e on r.expert_id = e.id " +
            "left join sys_user u on r.user_id = u.id " +
            "where r.batch_no in " +
            "<foreach collection='batchNos' item='bn' open='(' separator=',' close=')'>#{bn}</foreach>" +
            " order by r.extract_time desc" +
            "</script>")
    List<Map<String, Object>> selectExtractsByBatchNos(@Param("batchNos") List<String> batchNos);

}
