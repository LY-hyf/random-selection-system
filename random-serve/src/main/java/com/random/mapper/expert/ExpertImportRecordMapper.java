package com.random.mapper.expert;

import com.random.pojo.entity.expert.ExpertImportRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 专家导入记录数据访问接口。
 */
@Mapper
public interface ExpertImportRecordMapper {

    /**
     * 分页查询导入记录（配合 PageHelper 使用）。
     *
     * @return 导入记录列表
     */
    @Select("select r.id, r.file_name as fileName, r.total_count as totalCount, " +
            "r.success_count as successCount, r.fail_count as failCount, " +
            "u.username as username, r.create_time as createTime " +
            "from expert_import_record r left join sys_user u on r.user_id = u.id " +
            "order by r.id desc")
    List<Map<String, Object>> pageQuery();

    /**
     * 新增导入记录。
     *
     * @param record 导入记录实体
     * @return 受影响的行数
     */
    @Insert("insert into expert_import_record (file_name, file_url, total_count, success_count, fail_count, error_message, user_id, create_time) " +
            "values (#{fileName}, #{fileUrl}, #{totalCount}, #{successCount}, #{failCount}, #{errorMessage}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExpertImportRecord record);

}
