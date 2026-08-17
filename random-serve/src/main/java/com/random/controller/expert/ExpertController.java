package com.random.controller.expert;

import com.random.annotation.Log;
import com.random.pojo.dto.expert.ExpertPageRequest;
import com.random.pojo.dto.expert.ExtractHistoryPageRequest;
import com.random.pojo.dto.expert.ExtractRequest;
import com.random.pojo.entity.expert.ExpertInfo;
import com.random.pojo.vo.expert.ExpertVO;
import com.random.pojo.vo.expert.ExtractResultVO;
import com.random.result.PageResult;
import com.random.result.Result;
import com.random.service.expert.ExpertService;
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
import java.util.List;
import java.util.Map;

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
    @PreAuthorize("hasAuthority('expert:extract')")
    @PostMapping("/extract")
    public Result<ExtractResultVO> extract(@RequestBody ExtractRequest request) {
        log.info("随机抽取专家, applyType: {}, technicalType: {}, level: {}, count: {}",
                request.getApplyType(), request.getTechnicalType(), request.getLevel(), request.getCount());
        return Result.success(expertService.extract(request));
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
     *
     * @param expert 专家实体
     * @return 新增结果
     */
    @Log(module = "专家管理", operation = "新增专家")
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
     * Excel 导入专家。
     *
     * @param file 上传的 Excel 文件
     * @return 导入统计结果
     */
    @Log(module = "专家管理", operation = "Excel导入专家")
    @PreAuthorize("hasAuthority('expert:import')")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExperts(@RequestParam("file") MultipartFile file) {
        log.info("Excel 导入专家, fileName: {}", file.getOriginalFilename());
        return Result.success(expertService.importExperts(file));
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
