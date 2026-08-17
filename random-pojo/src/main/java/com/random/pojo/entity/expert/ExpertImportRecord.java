package com.random.pojo.entity.expert;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专家导入记录实体类。
 *
 * <p>对应数据库表 expert_import_record，记录专家批量导入的文件与结果统计。</p>
 */
@Data
public class ExpertImportRecord {

    /** 主键 ID */
    private Long id;

    /** 导入文件名 */
    private String fileName;

    /** 文件访问地址 */
    private String fileUrl;

    /** 导入总条数 */
    private Integer totalCount;

    /** 导入成功条数 */
    private Integer successCount;

    /** 导入失败条数 */
    private Integer failCount;

    /** 导入失败的错误信息 */
    private String errorMessage;

    /** 导入操作人 ID */
    private Long userId;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
