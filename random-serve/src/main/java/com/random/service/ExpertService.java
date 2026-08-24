package com.random.service;

import com.random.pojo.dto.ExpertPageRequest;
import com.random.pojo.dto.ExtractHistoryPageRequest;
import com.random.pojo.dto.ExtractRequest;
import com.random.pojo.entity.ExpertInfo;
import com.random.pojo.vo.ExpertVO;
import com.random.pojo.vo.ExtractResultVO;
import com.random.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 专家管理服务接口。
 *
 * <p>定义专家的查询、抽取、增删改、导入导出等业务能力。</p>
 */
public interface ExpertService {

    /**
     * 分页查询专家。
     *
     * @param request 分页及筛选条件
     * @return 专家分页结果
     */
    PageResult<ExpertVO> page(ExpertPageRequest request);

    /**
     * 获取筛选条件下拉数据。
     *
     * @return 申请类型、技术类型、等级等筛选选项
     */
    Map<String, Object> filterOptions();

    /**
     * 分页查询抽取历史。
     *
     * @param request 分页及筛选条件
     * @return 抽取历史分页结果
     */
    // TODO 功能待实现，目前无用
    PageResult<Map<String, Object>> extractHistory(ExtractHistoryPageRequest request);

    /**
     * 新增专家。
     *
     * @param expert 专家实体
     */
    void add(ExpertInfo expert);

    /**
     * 获取专家详情。
     *
     * @param id 专家 ID
     * @return 专家详情
     */
    ExpertInfo getById(Long id);

    /**
     * 编辑专家。
     *
     * @param id     专家 ID
     * @param expert 专家实体
     */
    void update(Long id, ExpertInfo expert);

    /**
     * 逻辑删除专家。
     *
     * @param id 专家 ID
     */
    void delete(Long id);

    /**
     * 同步Excel 导入专家，供异步提交导入专家任务进行复用。
     *
     * @param file 上传的 Excel 文件
     * @return 导入统计结果
     */
    Map<String, Object> importExperts(MultipartFile file);

    /**
     * 异步提交Execl导入专家任务
     * @param file
     * @return 立刻返回taskId
     */
    String submitImportAsyncTask(MultipartFile file);

    /**
     * 根据 taskId 查询导入结果。
     * @param taskId
     * @return 异步导入专家任务结果
     */
    Map<String, Object> getImportResult(String taskId);


    /**
     * 下载导入模板。
     *
     * @return Excel 模板文件字节数组
     */
    byte[] downloadTemplate();

    /**
     * 分页查询导入记录。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 导入记录分页结果
     */
    PageResult<Map<String, Object>> importRecords(int pageNum, int pageSize);

    /**
     * 随机抽取专家。
     *
     * @param request 抽取请求，包含抽取数量与筛选条件
     * @return 抽取结果
     */
    ExtractResultVO extract(ExtractRequest request);

    /**
     * 导出抽取专家结果。
     *
     * @param batchNo 抽取批次号
     * @return Excel 文件字节数组
     */
    byte[] exportExtractResult(String batchNo);

}
