package com.random.service;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘服务接口。
 *
 * <p>定义统计看板各维度数据的业务能力。</p>
 */
public interface DashboardService {

    /**
     * 获取统计汇总数据。
     *
     * @return 统计看板的汇总数据
     */
    Map<String, Object> getStatistics();

    /**
     * 获取技术类型分布。
     *
     * @return 技术类型分布列表
     */
    List<Map<String, Object>> getTechnicalDistribution();

    /**
     * 获取专家等级分布。
     *
     * @return 专家等级分布列表
     */
    List<Map<String, Object>> getLevelDistribution();

    /**
     * 获取申请类型分布。
     *
     * @return 申请类型分布列表
     */
    List<Map<String, Object>> getApplyTypeDistribution();

    /**
     * 获取抽取趋势。
     *
     * @return 抽取趋势数据
     */
    Map<String, Object> getExtractTrend();

    /**
     * 获取最新抽取记录。
     *
     * @param limit 返回的记录条数
     * @return 最新抽取记录列表
     */
    List<Map<String, Object>> getLatestExtracts(int limit);

    /**
     * 导出抽取记录（可指定批次号集合，为空时导出最近 10 条）。
     *
     * @param batchNos 要导出的批次号集合，为空则导出最近记录
     * @return Excel 文件字节数组
     */
    byte[] exportLatestExtracts(List<String> batchNos);

}
