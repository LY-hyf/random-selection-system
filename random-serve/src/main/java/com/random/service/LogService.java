package com.random.service;

import com.random.pojo.dto.LogPageRequest;
import com.random.pojo.entity.SysOperationLog;
import com.random.result.PageResult;

/**
 * 操作日志服务接口。
 *
 * <p>定义操作日志的分页查询与清空能力。</p>
 */
public interface LogService {

    /**
     * 分页查询操作日志。
     *
     * @param request 分页及筛选条件
     * @return 操作日志分页结果
     */
    PageResult<SysOperationLog> page(LogPageRequest request);

    /**
     * 清空操作日志。
     */
    void clear();

}
