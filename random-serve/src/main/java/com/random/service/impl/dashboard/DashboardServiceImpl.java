package com.random.service.impl.dashboard;

import com.random.mapper.dashboard.DashboardMapper;
import com.random.service.dashboard.DashboardService;
import com.random.utils.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现类。
 *
 * <p>实现统计看板各维度数据的查询与组装逻辑。</p>
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 仪表盘数据访问接口 */
    @Autowired
    private DashboardMapper dashboardMapper;

    /**
     * 获取统计汇总数据。
     *
     * @return 统计看板的汇总数据
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalExperts", dashboardMapper.countTotalExperts());
        result.put("monthNewExperts", dashboardMapper.countMonthNewExperts());
        result.put("monthExtractCount", dashboardMapper.countMonthExtract());
        result.put("onlineUsers", 1L);

        List<Map<String, Object>> trendList = dashboardMapper.selectRecentExtractTrend();
        result.put("recentExtractTrend", trendList != null ? trendList : new ArrayList<>());

        return result;
    }

    /**
     * 获取技术类型分布。
     *
     * @return 技术类型分布列表
     */
    @Override
    public List<Map<String, Object>> getTechnicalDistribution() {
        List<Map<String, Object>> list = dashboardMapper.selectTechnicalDistribution();
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 获取专家等级分布。
     *
     * @return 专家等级分布列表
     */
    @Override
    public List<Map<String, Object>> getLevelDistribution() {
        List<Map<String, Object>> list = dashboardMapper.selectLevelDistribution();
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 获取申请类型分布。
     *
     * @return 申请类型分布列表
     */
    @Override
    public List<Map<String, Object>> getApplyTypeDistribution() {
        List<Map<String, Object>> list = dashboardMapper.selectApplyTypeDistribution();
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 获取抽取趋势。
     *
     * <p>将查询结果拆分为日期与数量两个并列集合返回。</p>
     *
     * @return 抽取趋势数据
     */
    @Override
    public Map<String, Object> getExtractTrend() {
        List<Map<String, Object>> list = dashboardMapper.selectExtractTrend7Days();
        if (list == null) {
            list = new ArrayList<>();
        }
        List<String> dates = list.stream()
                .map(m -> (String) m.get("date"))
                .collect(Collectors.toList());
        List<Object> counts = list.stream()
                .map(m -> m.get("count"))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }

    /**
     * 获取最新抽取记录（按批次号分组，批次内嵌套专家明细）。
     *
     * @param limit 返回的批次条数
     * @return 最新抽取批次列表，每项含 batchNo、operator、extractTime、expertCount、experts
     */
    @Override
    public List<Map<String, Object>> getLatestExtracts(int limit) {
        List<Map<String, Object>> batches = dashboardMapper.selectLatestBatches(limit);
        if (batches == null || batches.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> batchNos = batches.stream()
                .map(b -> (String) b.get("batchNo"))
                .collect(Collectors.toList());
        List<Map<String, Object>> expertRows = dashboardMapper.getExpertsByBatchNos(batchNos);

        Map<String, List<Map<String, Object>>> expertsByBatch = new HashMap<>();
        if (expertRows != null) {
            for (Map<String, Object> row : expertRows) {
                String batchNo = (String) row.get("batchNo");
                expertsByBatch.computeIfAbsent(batchNo, k -> new ArrayList<>()).add(row);
            }
        }
        for (Map<String, Object> batch : batches) {
            String batchNo = (String) batch.get("batchNo");
            batch.put("experts", expertsByBatch.getOrDefault(batchNo, new ArrayList<>()));
        }
        return batches;
    }

    /**
     * 导出抽取记录为 Excel 字节数组。
     * <p>
     * 根据传入的批次号列表导出对应的抽取记录；若批次号列表为空，则默认导出最近 10 条抽取记录。
     * 导出的 Excel 包含批次号、专家姓名、操作人、抽取时间四列，数据来源于统计看板（Dashboard）数据访问层。
     *
     * @param batchNos 要导出的批次号集合；允许为空，为空时自动导出最近 10 条记录。
     *                 若不为空，则只导出这些批次号对应的记录，忽略不存在或无效的批次号。
     * @return Excel 文件的字节数组，可直接用于下载或响应流输出；若查询无数据，则返回仅含表头的空 Excel 文件。
     * @implNote 该方法调用 {@link ExcelUtil#create(String, String[], List)} 生成 Excel 工作簿，
     *           生成的 Excel 为 .xlsx 格式，无样式，适用于中小规模数据导出。
     */
    @Override
    public byte[] exportLatestExtracts(List<String> batchNos) {
        List<Map<String, Object>> list;
        if (batchNos == null || batchNos.isEmpty()) {
            list = dashboardMapper.selectLatestExtracts(10);
        } else {
            list = dashboardMapper.selectExtractsByBatchNos(batchNos);
        }
        String[] headers = {"批次号", "专家姓名", "操作人", "抽取时间"};
        List<List<String>> rows = new ArrayList<>();
        if (list != null) {
            for (Map<String, Object> item : list) {
                List<String> row = new ArrayList<>();
                row.add(str(item.get("batchNo")));
                row.add(str(item.get("expertName")));
                row.add(str(item.get("operator")));
                row.add(str(item.get("extractTime")));
                rows.add(row);
            }
        }
        return ExcelUtil.create("抽取记录", headers, rows);
    }

    /**
     * 将对象转为字符串，null 转为空字符串。
     */
    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
