package com.random.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.random.mapper.log.SysOperationLogMapper;
import com.random.pojo.dto.LogPageRequest;
import com.random.pojo.entity.SysOperationLog;
import com.random.result.PageResult;
import com.random.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现类。
 *
 * <p>实现操作日志的分页查询与清空逻辑。</p>
 */
@Service
@Slf4j
public class LogServiceImpl implements LogService {

    /** 操作日志数据访问接口 */
    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询操作日志。
     *
     * @param request 分页及筛选条件
     * @return 操作日志分页结果
     */
    @Override
    public PageResult<SysOperationLog> page(LogPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysOperationLog> list = sysOperationLogMapper.pageQuery(request);
        PageInfo<SysOperationLog> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 清空操作日志。
     */
    @Override
    public void clear() {
        sysOperationLogMapper.clear();
        log.info("清空日志成功");
    }

}
