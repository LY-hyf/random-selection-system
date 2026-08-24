package com.random.controller;

import com.random.pojo.dto.LogPageRequest;
import com.random.pojo.entity.SysOperationLog;
import com.random.result.PageResult;
import com.random.result.Result;
import com.random.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器。
 *
 * <p>提供操作日志的分页查询与清空接口。</p>
 */
@RestController
@RequestMapping("/logs")
@Slf4j
public class LogController {

    /** 操作日志服务 */
    @Autowired
    private LogService logService;

    /**
     * 分页查询操作日志。
     *
     * @param request 分页及筛选条件
     * @return 操作日志分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<SysOperationLog>> page(LogPageRequest request) {
        log.debug("分页查询操作日志, pageNum: {}, pageSize: {}", request.getPageNum(), request.getPageSize());
        return Result.success(logService.page(request));
    }

    /**
     * 清空日志（清空操作本身不记录日志）。
     *
     * @return 清空结果
     */
    @PreAuthorize("hasAuthority('log:clear')")
    @DeleteMapping("/clear")
    public Result clear() {
        log.info("清空操作日志");
        logService.clear();
        return Result.success();
    }

}
