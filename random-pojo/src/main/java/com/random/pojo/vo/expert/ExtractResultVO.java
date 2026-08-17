package com.random.pojo.vo.expert;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 随机抽取结果视图对象。
 */
@Data
public class ExtractResultVO {

    /** 抽取批次号 */
    private String batchNo;

    /** 抽取时间 */
    private LocalDateTime extractTime;

    /** 是否来自缓存 */
    private Boolean isFromCache;

    /** 抽中的专家列表（精简字段） */
    private List<ExtractExpertVO> experts;
}
