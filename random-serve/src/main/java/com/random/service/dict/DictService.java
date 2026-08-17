package com.random.service.dict;

import com.random.pojo.entity.dict.SysDict;

import java.util.List;
import java.util.Map;

/**
 * 数据字典服务接口。
 *
 * <p>定义字典项的查询、新增、编辑与删除等业务能力。</p>
 */
public interface DictService {

    /**
     * 按类型查询字典项。
     *
     * @param dictType 字典类型
     * @return 该类型下的字典项列表
     */
    List<SysDict> getByType(String dictType);

    /**
     * 查询所有字典项（按类型分组）。
     *
     * @return 按字典类型分组的字典项
     */
    Map<String, List<SysDict>> all();

    /**
     * 新增字典项。
     *
     * @param dict 字典实体
     */
    void add(SysDict dict);

    /**
     * 编辑字典项。
     *
     * @param id   字典 ID
     * @param dict 字典实体
     */
    void update(Long id, SysDict dict);

    /**
     * 删除字典项。
     *
     * @param id 字典 ID
     */
    void delete(Long id);

}
