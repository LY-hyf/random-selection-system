package com.random.dto.expert;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 专家 Excel 导入行 DTO，按列序号映射（与导入模板列顺序一致）。
 */
@Data
public class ExpertImportRow {

    /** 姓名 */
    @ExcelProperty(index = 0)
    private String name;

    /** 出生年月 */
    @ExcelProperty(index = 1)
    private String birthday;

    /** 学历 */
    @ExcelProperty(index = 2)
    private String education;

    /** 工作单位 */
    @ExcelProperty(index = 3)
    private String company;

    /** 申报类型 */
    @ExcelProperty(index = 4)
    private String applyType;

    /** 技术类型 */
    @ExcelProperty(index = 5)
    private String technicalType;

    /** 级别 */
    @ExcelProperty(index = 6)
    private String level;

    /** 联系方式 */
    @ExcelProperty(index = 7)
    private String phone;
}
