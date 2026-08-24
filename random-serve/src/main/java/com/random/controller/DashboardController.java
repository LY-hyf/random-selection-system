package com.random.controller;

import com.random.result.Result;
import com.random.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘控制器。
 *
 * <p>提供统计看板相关的数据查询接口。</p>
 */
@RestController
@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {

    /** 仪表盘服务 */
    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取统计数据接口。
     *
     * @return 统计看板的汇总数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        log.info("获取统计数据");
        return Result.success(dashboardService.getStatistics());
    }

    /**
     * 获取技术类型分布接口。
     *
     * @return 技术类型分布数据
     */
    @GetMapping("/technical-distribution")
    public Result<List<Map<String, Object>>> technicalDistribution() {
        log.debug("获取技术类型分布");
        return Result.success(dashboardService.getTechnicalDistribution());
    }

    /**
     * 获取等级分布接口。
     *
     * @return 专家等级分布数据
     */
    @GetMapping("/level-distribution")
    public Result<List<Map<String, Object>>> levelDistribution() {
        log.debug("获取等级分布");
        return Result.success(dashboardService.getLevelDistribution());
    }

    /**
     * 获取申请类型分布接口。
     *
     * @return 申请类型分布数据
     */
    @GetMapping("/apply-type-distribution")
    public Result<List<Map<String, Object>>> applyTypeDistribution() {
        log.debug("获取申请类型分布");
        return Result.success(dashboardService.getApplyTypeDistribution());
    }

    /**
     * 获取抽取趋势接口。
     *
     * @return 近期的抽取趋势数据
     */
    @GetMapping("/extract-trend")
    public Result<Map<String, Object>> extractTrend() {
        log.debug("获取抽取趋势");
        return Result.success(dashboardService.getExtractTrend());
    }

    /**
     * 获取最新抽取记录接口。
     *
     * @param limit 返回的记录条数，默认 10
     * @return 最新抽取记录列表
     */
    @GetMapping("/latest-extracts")
    public Result<List<Map<String, Object>>> latestExtracts(@RequestParam(defaultValue = "10") int limit) {
        log.info("获取最新抽取记录, limit: {}", limit);
        return Result.success(dashboardService.getLatestExtracts(limit));
    }

    /**
     * 导出抽取记录。
     *
     * <p>可通过 batchNos 指定要导出的批次（逗号分隔），为空时导出最近的抽取记录。</p>
     *
     * @param batchNos 要导出的批次号集合，可为空
     * @param response HTTP 响应对象，用于写出 Excel 文件
     * @throws IOException 写出响应流失败时抛出
     */
    @GetMapping("/latest-extracts/export")
    public void exportLatestExtracts(@RequestParam(value = "batchNos", required = false) List<String> batchNos,
                                     HttpServletResponse response) throws IOException {
        log.info("导出抽取记录, batchNos: {}", batchNos);
        byte[] data = dashboardService.exportLatestExtracts(batchNos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=latest_extracts.xlsx");
        response.getOutputStream().write(data);
    }

}
