package com.random.controller;

import com.random.annotation.Log;
import com.random.context.BaseContext;
import com.random.pojo.dto.ExpertPageRequest;
import com.random.pojo.dto.ExtractHistoryPageRequest;
import com.random.pojo.dto.ExtractRequest;
import com.random.pojo.entity.ExpertInfo;
import com.random.pojo.vo.ExpertVO;
import com.random.pojo.vo.ExtractResultVO;
import com.random.result.PageResult;
import com.random.result.Result;
import com.random.service.ExpertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 专家管理控制器。
 *
 * <p>提供专家信息的查询、抽取、增删改、导入导出等接口。</p>
 */
@RestController
@RequestMapping("/experts")
@Slf4j
public class ExpertController {

    /** 专家管理服务 */
    @Autowired
    private ExpertService expertService;

    /**
     * 分页查询专家列表。
     *
     * @param request 分页及筛选条件
     * @return 专家分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<ExpertVO>> page(ExpertPageRequest request) {
        log.debug("分页查询专家列表, pageNum: {}, pageSize: {}", request.getPageNum(), request.getPageSize());
        return Result.success(expertService.page(request));
    }

    /**
     * 获取专家筛选条件下拉数据。
     *
     * @return 申请类型、技术类型、等级等筛选选项
     */
    @GetMapping("/filter-options")
    public Result<Map<String, Object>> filterOptions() {
        log.debug("获取专家筛选条件下拉数据");
        return Result.success(expertService.filterOptions());
    }

    /**
     * 获取抽取历史记录。
     *
     * @param request 分页及筛选条件
     * @return 抽取历史分页结果
     */
    @GetMapping("/extract-history")
    public Result<PageResult<Map<String, Object>>> extractHistory(ExtractHistoryPageRequest request) {
        log.debug("获取抽取历史记录, pageNum: {}, pageSize: {}", request.getPageNum(), request.getPageSize());
        return Result.success(expertService.extractHistory(request));
    }

    /**
     * 随机抽取专家。
     *
     * @param request 抽取请求，包含抽取数量与筛选条件
     * @return 抽取结果
     */
    @Log(module = "专家管理", operation = "随机抽取专家")
    // 方法级权限控制,在请求执行前判断当前登录用户是否拥有指定的权限字符串
    @PreAuthorize("hasAuthority('expert:extract')")
    @PostMapping("/extract")
    public Result<ExtractResultVO> extract(@RequestBody ExtractRequest request) {
        // 主线程获取 userId
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未获取到用户信息，请重新登录");
        }
        log.info("提交异步任务, applyType: {}, technicalType: {}, level: {}, count: {}",
                request.getApplyType(), request.getTechnicalType(), request.getLevel(), request.getCount());
        // 调用异步抽取
        CompletableFuture<ExtractResultVO> future = expertService.extractAsync(request,userId);
        // 设置超时（防止长时间阻塞）
        try {
            ExtractResultVO result = null;
            try {
                result = future.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            return Result.success(result);
        } catch (TimeoutException e) {
            log.warn("抽取超时，降级返回处理中状态");
            return Result.error("抽取任务正在处理，请稍后刷新查看结果");
        } catch (RejectedExecutionException e) {
            log.error("抽取失败", e);
            return Result.error("业务繁忙，抽取失败，请稍后再试");
        }
    }

    /**
     * 导出抽取专家结果。
     *
     * @param batchNo  抽取批次号
     * @param response HTTP 响应对象，用于写出 Excel 文件
     * @throws IOException 写出响应流失败时抛出
     */
    @GetMapping("/export")
    public void exportExtractResult(@RequestParam("batchNo") String batchNo, HttpServletResponse response) throws IOException {
        log.info("导出抽取专家结果, batchNo: {}", batchNo);
        byte[] data = expertService.exportExtractResult(batchNo);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=extract_result.xlsx");
        response.getOutputStream().write(data);
    }

    /**
     * 新增专家。
     * @@Pattern（新增到 ExpertInfo.phone）+ @Valid 挂到 ExpertController
     * @param expert 专家实体
     * @return 新增结果
     */
    @Log(module = "专家管理", operation = "新增专家")
    // 方法级权限控制,在请求执行前判断当前登录用户是否拥有指定的权限字符串。
    @PreAuthorize("hasAuthority('expert:add:submit')")
    @PostMapping
    public Result add(@Valid @RequestBody ExpertInfo expert) {
        log.info("新增专家, name: {}", expert.getName());
        expertService.add(expert);
        return Result.success();
    }

    /**
     * 获取专家详情。
     *
     * @param id 专家 ID
     * @return 专家详情
     */
    @GetMapping("/{id}")
    public Result<ExpertInfo> getById(@PathVariable Long id) {
        log.debug("获取专家详情, id: {}", id);
        return Result.success(expertService.getById(id));
    }

    /**
     * 编辑专家。
     *
     * @param id     专家 ID
     * @param expert 专家实体
     * @return 编辑结果
     */
    @Log(module = "专家管理", operation = "编辑专家")
    @PreAuthorize("hasAuthority('expert:edit')")
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody ExpertInfo expert) {
        log.info("编辑专家, id: {}", id);
        expertService.update(id, expert);
        return Result.success();
    }

    /**
     * 删除专家（逻辑删除）。
     *
     * @param id 专家 ID
     * @return 删除结果
     */
    @Log(module = "专家管理", operation = "删除专家")
    @PreAuthorize("hasAuthority('expert:delete')")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("删除专家, id: {}", id);
        expertService.delete(id);
        return Result.success();
    }

    /**
     * 异步 Excel 导入专家。
     *
     * @param file 上传的 Excel 文件
     * @return 导入统计结果
     */
    @Log(module = "专家管理", operation = "异步Excel导入专家")
    @PreAuthorize("hasAuthority('expert:import')")
    @PostMapping("/import")
    public Result<String> importExperts(@RequestParam("file") MultipartFile file)
            throws ExecutionException,InterruptedException, TimeoutException {
        log.info("导入专家, fileName: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());
        try{
            // 提交异步任务，释放主线程
            String taskId = expertService.submitImportAsyncTask(file);
            log.info("异步导入任务已提交, taskId: {}", taskId);
            return Result.success(taskId);
        } catch (RejectedExecutionException e) {
            log.warn("异步任务堆积过多，拒绝新任务");
            return Result.error("系统繁忙，请稍后再试");
        }
    }

    /**
     * 查询导入任务结果（轮询接口）。
     *
     * @param taskId 任务 ID
     * @return 任务状态及结果
     */
    @GetMapping("/import/result/{taskId}")
    public Result<Map<String, Object>> getImportResult(@PathVariable String taskId) {
        log.debug("查询导入结果, taskId: {}", taskId);
        Map<String, Object> result = expertService.getImportResult(taskId);
        return Result.success(result);
    }

    /**
     * 下载导入模板。
     *
     * @param response HTTP 响应对象，用于写出 Excel 模板文件
     * @throws IOException 写出响应流失败时抛出
     */
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        log.info("下载专家导入模板");
        byte[] data = expertService.downloadTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=expert_template.xlsx");
        response.getOutputStream().write(data);
    }

    /**
     * 获取导入历史记录。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 导入历史分页结果
     */
    @GetMapping("/import-records")
    public Result<PageResult<Map<String, Object>>> importRecords(@RequestParam(defaultValue = "1") int pageNum,
                                                                  @RequestParam(defaultValue = "10") int pageSize) {
        log.debug("获取导入历史记录, pageNum: {}, pageSize: {}", pageNum, pageSize);
        return Result.success(expertService.importRecords(pageNum, pageSize));
    }

}
