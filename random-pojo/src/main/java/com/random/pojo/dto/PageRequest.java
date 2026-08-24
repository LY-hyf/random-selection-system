package com.random.pojo.dto;

import lombok.Data;

/**
 * 分页查询请求，含通用分页参数与用户筛选条件。
 */
@Data
public class PageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 用户名（模糊匹配） */
    private String username;

    /** 昵称（模糊匹配） */
    private String nickname;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /**
     * 获取每页条数（限制上限，防止过大分页拖慢查询）。
     *
     * @return 修正后的每页条数，上限 100
     */
    public Integer getPageSize() {
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return Math.min(size, 100);
    }

    /**
     * 计算分页偏移量。
     *
     * @return SQL LIMIT 子句的起始偏移量
     */
    public int getOffset() {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        return (num - 1) * getPageSize();
    }
}
