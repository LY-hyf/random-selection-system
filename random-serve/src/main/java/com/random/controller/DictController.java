package com.random.controller;

import com.random.annotation.Log;
import com.random.pojo.entity.SysDict;
import com.random.result.Result;
import com.random.service.DictService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据字典控制器。
 *
 * <p>提供字典数据的查询、新增、编辑与删除接口。</p>
 */
@RestController
@RequestMapping("/dicts")
@Slf4j
public class DictController {

    /** 数据字典服务 */
    @Autowired
    private DictService dictService;

    /**
     * 按类型获取字典数据。
     *
     * @param dictType 字典类型
     * @return 该类型下的字典数据列表
     */
    @GetMapping("/type/{dictType}")
    public Result<List<SysDict>> getByType(@PathVariable String dictType) {
        log.debug("按类型获取字典数据, dictType: {}", dictType);
        return Result.success(dictService.getByType(dictType));
    }

    /**
     * 获取所有字典数据（按类型分组）。
     *
     * @return 按字典类型分组的字典数据
     */
    @GetMapping("/all")
    public Result<Map<String, List<SysDict>>> all() {
        log.debug("获取所有字典数据");
        return Result.success(dictService.all());
    }

    /**
     * 新增字典。
     *
     * @param dict 字典实体
     * @return 新增结果
     */
    @Log(module = "数据字典", operation = "新增字典")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public Result add(@RequestBody SysDict dict) {
        log.info("新增字典, dictType: {}, dictCode: {}", dict.getDictType(), dict.getDictCode());
        dictService.add(dict);
        return Result.success();
    }

    /**
     * 编辑字典。
     *
     * @param id   字典 ID
     * @param dict 字典实体
     * @return 编辑结果
     */
    @Log(module = "数据字典", operation = "编辑字典")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody SysDict dict) {
        log.info("编辑字典, id: {}", id);
        dictService.update(id, dict);
        return Result.success();
    }

    /**
     * 删除字典。
     *
     * @param id 字典 ID
     * @return 删除结果
     */
    @Log(module = "数据字典", operation = "删除字典")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("删除字典, id: {}", id);
        dictService.delete(id);
        return Result.success();
    }

}
