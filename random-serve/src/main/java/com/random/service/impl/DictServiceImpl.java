package com.random.service.impl;

import com.random.constant.StatusConstant;
import com.random.mapper.dict.SysDictMapper;
import com.random.pojo.entity.SysDict;
import com.random.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典服务实现类。
 *
 * <p>实现字典项的查询、新增、编辑与删除等业务逻辑。</p>
 */
@Service
@Slf4j
public class DictServiceImpl implements DictService {

    /** 数据字典数据访问接口 */
    @Autowired
    private SysDictMapper sysDictMapper;

    /**
     * 按类型查询字典项。
     *
     * @param dictType 字典类型
     * @return 该类型下的字典项列表，无数据时返回空列表
     */
    @Override
    public List<SysDict> getByType(String dictType) {
        List<SysDict> list = sysDictMapper.getByType(dictType);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 查询所有字典项并按类型分组。
     *
     * @return 按字典类型分组的字典项映射
     */
    @Override
    public Map<String, List<SysDict>> all() {
        List<SysDict> list = sysDictMapper.getAll();
        Map<String, List<SysDict>> result = new LinkedHashMap<>();
        if (list != null) {
            for (SysDict dict : list) {
                result.computeIfAbsent(dict.getDictType(), k -> new ArrayList<>()).add(dict);
            }
        }
        return result;
    }

    /**
     * 新增字典项。
     *
     * @param dict 字典实体
     */
    @Override
    public void add(SysDict dict) {
        dict.setSort(dict.getSort() == null ? 0 : dict.getSort());
        dict.setStatus(StatusConstant.ENABLE);
        dict.setCreateTime(LocalDateTime.now());
        sysDictMapper.insert(dict);
        log.info("新增字典成功，id: {}, dictType: {}, dictCode: {}", dict.getId(), dict.getDictType(), dict.getDictCode());
    }

    /**
     * 编辑字典项。
     *
     * @param id   字典 ID
     * @param dict 字典实体
     */
    @Override
    public void update(Long id, SysDict dict) {
        dict.setId(id);
        sysDictMapper.update(dict);
        log.info("编辑字典成功，id: {}", id);
    }

    /**
     * 删除字典项。
     *
     * @param id 字典 ID
     */
    @Override
    public void delete(Long id) {
        sysDictMapper.deleteById(id);
        log.info("删除字典成功，id: {}", id);
    }

}
