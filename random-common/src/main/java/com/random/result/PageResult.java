package com.random.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装类。
 *
 * @param <T> 数据记录类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private long total;

    /** 当前页的数据记录集合 */
    private List<T> records;
}
