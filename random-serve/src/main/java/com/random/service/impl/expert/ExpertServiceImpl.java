package com.random.service.impl.expert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.random.constant.StatusConstant;
import com.random.context.BaseContext;
import com.random.exception.BaseException;
import com.random.exception.LoginFailedException;
import com.random.mapper.expert.ExpertExtractRecordMapper;
import com.random.mapper.expert.ExpertImportRecordMapper;
import com.random.mapper.expert.ExpertInfoMapper;
import com.random.mapper.dict.SysDictMapper;
import com.random.pojo.dto.expert.ExpertPageRequest;
import com.random.pojo.dto.expert.ExtractHistoryPageRequest;
import com.random.pojo.dto.expert.ExtractRequest;
import com.random.pojo.entity.expert.ExpertExtractRecord;
import com.random.pojo.entity.expert.ExpertImportRecord;
import com.random.pojo.entity.expert.ExpertInfo;
import com.random.pojo.entity.dict.SysDict;
import com.random.pojo.vo.expert.ExpertVO;
import com.random.pojo.vo.expert.ExtractExpertVO;
import com.random.pojo.vo.expert.ExtractResultVO;
import com.random.result.PageResult;
import com.random.service.expert.ExpertService;
import com.random.utils.ExcelUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 专家管理服务实现类。
 *
 * <p>实现专家的查询、抽取、增删改、导入导出等业务逻辑，
 * 抽取操作使用本地缓存与锁保证同一用户短时间内抽取结果一致。</p>
 */
@Service
@Slf4j
public class ExpertServiceImpl implements ExpertService {

    /** 专家信息数据访问接口 */
    @Autowired
    private ExpertInfoMapper expertInfoMapper;

    /** 专家抽取记录数据访问接口 */
    @Autowired
    private ExpertExtractRecordMapper expertExtractRecordMapper;

    /** 专家导入记录数据访问接口 */
    @Autowired
    private ExpertImportRecordMapper expertImportRecordMapper;

    /** 数据字典数据访问接口 */
    @Autowired
    private SysDictMapper sysDictMapper;

    /** Redis 字符串模板，用于存储抽取结果缓存 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** JSON 序列化工具，用于缓存对象的读写 */
    @Autowired
    private ObjectMapper objectMapper;

    /** 抽取结果缓存键前缀 */
    private static final String EXTRACT_CACHE_PREFIX = "extract:";

    /** 抽取结果缓存过期时间（分钟） */
    private static final long EXTRACT_CACHE_TTL_MINUTES = 5;

    /** 抽取锁，用于串行化抽取操作，防止并发重复抽取 */
    private final ReentrantLock extractLock = new ReentrantLock();

    /** 本地缓存（L1），用于快速命中，避免频繁访问 Redis */
    private final Cache<String, ExtractResultVO> localCache = Caffeine.newBuilder()
            .expireAfterWrite(EXTRACT_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    /**
     * 分页查询专家。
     *
     * <p>查询专家分页数据，并填充各字典编码对应的中文标签及最近抽取状态。</p>
     *
     * @param request 分页及筛选条件
     * @return 专家分页结果
     */
    @Override
    public PageResult<ExpertVO> page(ExpertPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<ExpertInfo> list = expertInfoMapper.pageQuery(request);
        PageInfo<ExpertInfo> pageInfo = new PageInfo<>(list);

        List<ExpertInfo> experts = pageInfo.getList();
        List<ExpertVO> records = new ArrayList<>();
        if (experts != null && !experts.isEmpty()) {
            Map<String, String> applyTypeLabelMap = buildDictLabelMap("apply_type");
            Map<String, String> technicalTypeLabelMap = buildDictLabelMap("technical_type");
            Map<String, String> levelLabelMap = buildDictLabelMap("level");
            Map<String, String> educationLabelMap = buildDictLabelMap("education");
            Map<Long, LocalDateTime> lastExtractTimeMap = buildLastExtractTimeMap(
                    experts.stream().map(ExpertInfo::getId).collect(Collectors.toList()));

            for (ExpertInfo expert : experts) {
                ExpertVO vo = new ExpertVO();
                vo.setId(expert.getId());
                vo.setName(expert.getName());
                vo.setBirthday(expert.getBirthday());
                vo.setEducation(expert.getEducation());
                vo.setEducationLabel(educationLabelMap.get(expert.getEducation()));
                vo.setCompany(expert.getCompany());
                vo.setApplyType(expert.getApplyType());
                vo.setApplyTypeLabel(applyTypeLabelMap.get(expert.getApplyType()));
                vo.setTechnicalType(expert.getTechnicalType());
                vo.setTechnicalTypeLabel(technicalTypeLabelMap.get(expert.getTechnicalType()));
                vo.setLevel(expert.getLevel());
                vo.setLevelLabel(levelLabelMap.get(expert.getLevel()));
                vo.setPhone(expert.getPhone());
                vo.setStatus(expert.getStatus());
                LocalDateTime lastExtractTime = lastExtractTimeMap.get(expert.getId());
                vo.setLastExtractTime(lastExtractTime);
                vo.setExtractStatus(computeExtractStatus(lastExtractTime));
                records.add(vo);
            }
        }

        return new PageResult<>(pageInfo.getTotal(), records);
    }

    /**
     * 获取筛选条件下拉数据。
     *
     * @return 申请类型、技术类型、等级等筛选选项
     */
    @Override
    public Map<String, Object> filterOptions() {
        Map<String, Object> result = new HashMap<>();
        result.put("applyTypes", nullToEmpty(expertInfoMapper.getDistinctApplyTypes()));
        result.put("technicalTypes", nullToEmpty(expertInfoMapper.getDistinctTechnicalTypes()));
        result.put("levels", nullToEmpty(expertInfoMapper.getDistinctLevels()));
        return result;
    }

    /**
     * 分页查询抽取历史。
     *
     * @param request 分页及筛选条件
     * @return 抽取历史分页结果
     */
    @Override
    public PageResult<Map<String, Object>> extractHistory(ExtractHistoryPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<Map<String, Object>> list = expertExtractRecordMapper.pageQuery(request);
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 新增专家。
     *
     * @param expert 专家实体
     */
    @Override
    public void add(ExpertInfo expert) {
        expert.setStatus(expert.getStatus() == null ? StatusConstant.ENABLE : expert.getStatus());
        expert.setDeleted(0);
        expert.setCreateTime(LocalDateTime.now());
        expertInfoMapper.insert(expert);
    }

    /**
     * 获取专家详情。
     *
     * @param id 专家 ID
     * @return 专家详情，未找到时返回 null
     */
    @Override
    public ExpertInfo getById(Long id) {
        return expertInfoMapper.getById(id);
    }

    /**
     * 编辑专家。
     *
     * @param id     专家 ID
     * @param expert 专家实体
     */
    @Override
    public void update(Long id, ExpertInfo expert) {
        expert.setId(id);
        expertInfoMapper.update(expert);
    }

    /**
     * 逻辑删除专家。
     *
     * @param id 专家 ID
     */
    @Override
    public void delete(Long id) {
        expertInfoMapper.logicalDelete(id);
    }

    /**
     * Excel 导入专家。
     *
     * <p>解析上传的 Excel，逐行校验并写入，记录导入统计结果。</p>
     *
     * @param file 上传的 Excel 文件
     * @return 导入统计结果，包含总数、成功数、失败数及失败明细
     */
    @Override
    public Map<String, Object> importExperts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LoginFailedException("上传文件不能为空");
        }
        List<ExpertInfo> experts = parseExcel(file);

        // 过滤示例行：name 包含「示例」或「请删除」的行自动跳过，不参与导入
        int parsedCount = experts.size();
        experts.removeIf(e -> e.getName() != null
                && (e.getName().contains("示例") || e.getName().contains("请删除")));
        if (experts.size() < parsedCount) {
            log.info("跳过示例行 {} 条", parsedCount - experts.size());
        }

        // 构建 中文标签 -> 编码 的反向映射，兼容全中文 Excel 导入（也兼容已是编码的情况）
        Map<String, String> applyTypeCodeMap = buildDictCodeMap("apply_type");
        Map<String, String> technicalTypeCodeMap = buildDictCodeMap("technical_type");
        Map<String, String> levelCodeMap = buildDictCodeMap("level");
        Map<String, String> educationCodeMap = buildDictCodeMap("education");

        int totalCount = experts.size();
        int successCount = 0;
        List<Map<String, Object>> failDetails = new ArrayList<>();

        for (int i = 0; i < experts.size(); i++) {
            ExpertInfo expert = experts.get(i);
            // 中文标签统一转编码，保证与「实体存编码」方案一致
            expert.setApplyType(toCode(expert.getApplyType(), applyTypeCodeMap));
            expert.setTechnicalType(toCode(expert.getTechnicalType(), technicalTypeCodeMap));
            expert.setLevel(toCode(expert.getLevel(), levelCodeMap));
            expert.setEducation(toCode(expert.getEducation(), educationCodeMap));

            int rowNum = i + 2; // 数据从第 2 行开始（第 1 行为表头）
            String reason = validateExpert(expert);
            if (reason != null) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("row", rowNum);
                fail.put("name", expert.getName());
                fail.put("reason", reason);
                failDetails.add(fail);
                continue;
            }
            expert.setStatus(StatusConstant.ENABLE);
            expert.setDeleted(0);
            expert.setCreateTime(LocalDateTime.now());
            expertInfoMapper.insert(expert);
            successCount++;
        }

        ExpertImportRecord record = new ExpertImportRecord();
        record.setFileName(file.getOriginalFilename());
        record.setTotalCount(totalCount);
        record.setSuccessCount(successCount);
        record.setFailCount(totalCount - successCount);
        record.setUserId(BaseContext.getCurrentId());
        record.setCreateTime(LocalDateTime.now());
        expertImportRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", totalCount - successCount);
        result.put("recordId", record.getId());
        result.put("failDetails", failDetails);

        log.info("专家导入完成，文件: {}, 总数: {}, 成功: {}, 失败: {}",
                file.getOriginalFilename(), totalCount, successCount, totalCount - successCount);
        return result;
    }

    /**
     * 下载导入模板。
     *
     * @return Excel 模板文件字节数组
     */
    @Override
    public byte[] downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("专家信息");
            String[] headers = {"姓名", "出生年月", "学历", "工作单位", "申报类型", "技术类型", "级别", "联系方式"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 示例数据行（类型字段用中文标签，导入时会自动转编码）；正式上传前请删除本行
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("张三（示例，请删除）");
            sampleRow.createCell(1).setCellValue("1980-01-01");
            sampleRow.createCell(2).setCellValue("本科");
            sampleRow.createCell(3).setCellValue("朔州市示例单位");
            sampleRow.createCell(4).setCellValue("医疗");
            sampleRow.createCell(5).setCellValue("临床医学");
            sampleRow.createCell(6).setCellValue("高级");
            sampleRow.createCell(7).setCellValue("13800000000");

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new LoginFailedException("模板生成失败");
        }
    }

    /**
     * 分页查询导入记录。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 导入记录分页结果
     */
    @Override
    public PageResult<Map<String, Object>> importRecords(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Map<String, Object>> list = expertImportRecordMapper.pageQuery();
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 随机抽取专家。
     *
     * <p>核心流程：先查多级缓存（Caffeine + Redis）→ 未命中则加锁串行抽取，
     * 抽取结果写入缓存，保证同一用户短时间内重复抽取返回一致结果。</p>
     *
     * <p>业务规则：
     * <ul>
     *   <li>结果集 = 满足「申报类型 AND 技术类型 AND 级别」交集、且近 30 天未被抽取的专家；</li>
     *   <li>结果集内随机打乱后取前 N 位（N 由 count 指定，默认 5，上限 20）；</li>
     *   <li>抽取记录落库，作为 30 天去重的依据。</li>
     * </ul></p>
     *
     * @param request 抽取请求，包含抽取数量与筛选条件
     * @return 抽取结果
     */
    @Override
    public ExtractResultVO extract(ExtractRequest request) {
        Long userId = BaseContext.getCurrentId();
        String cacheKey = userId + ":" + request.getApplyType() + ":" + request.getTechnicalType() + ":" + request.getLevel();

        // 先查缓存（无锁快速路径），命中则直接返回
        ExtractResultVO cached = getCached(cacheKey);
        if (cached != null) {
            log.info("抽取命中缓存，batchNo: {}", cached.getBatchNo());
            return copyAsFromCache(cached);
        }

        // 加锁，防止并发重复抽取同一批专家
        extractLock.lock();
        try {
            // 双重检查：拿到锁后可能已被其他线程抽取并写入缓存
            cached = getCached(cacheKey);
            if (cached != null) {
                log.info("抽取命中缓存（并发等待后），batchNo: {}", cached.getBatchNo());
                return copyAsFromCache(cached);
            }

            List<ExpertInfo> experts = expertInfoMapper.getExtractableExperts(
                    request.getApplyType(), request.getTechnicalType(), request.getLevel());
            if (experts == null || experts.isEmpty()) {
                throw new BaseException("没有符合条件的专家");
            }

            Collections.shuffle(experts);
            int requestCount = request.getCount() == null || request.getCount() < 1 ? 5 : request.getCount();
            requestCount = Math.min(requestCount, 20);
            int count = Math.min(experts.size(), requestCount);
            List<ExpertInfo> selected = new ArrayList<>(experts.subList(0, count));

            String batchNo = "EX-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            LocalDateTime now = LocalDateTime.now();
            for (ExpertInfo expert : selected) {
                ExpertExtractRecord record = new ExpertExtractRecord();
                record.setBatchNo(batchNo);
                record.setExpertId(expert.getId());
                record.setUserId(userId);
                record.setApplyType(expert.getApplyType());
                record.setTechnicalType(expert.getTechnicalType());
                record.setLevel(expert.getLevel());
                record.setExtractTime(now);
                record.setCreateTime(now);
                expertExtractRecordMapper.insert(record);
            }

            Map<String, String> applyTypeMap = buildDictLabelMap("apply_type");
            Map<String, String> technicalTypeMap = buildDictLabelMap("technical_type");
            Map<String, String> levelMap = buildDictLabelMap("level");

            ExtractResultVO result = new ExtractResultVO();
            result.setBatchNo(batchNo);
            result.setExtractTime(now);
            result.setIsFromCache(false);
            result.setExperts(selected.stream()
                    .map(e -> toExtractExpertVO(e, applyTypeMap, technicalTypeMap, levelMap))
                    .collect(Collectors.toList()));

            putCache(cacheKey, result);
            log.info("抽取完成，batchNo: {}, 抽取人数: {}, 条件: applyType={}, technicalType={}, level={}",
                    batchNo, selected.size(), request.getApplyType(), request.getTechnicalType(), request.getLevel());
            return result;
        } finally {
            extractLock.unlock();
        }
    }

    private ExtractResultVO copyAsFromCache(ExtractResultVO cached) {
        ExtractResultVO result = new ExtractResultVO();
        result.setBatchNo(cached.getBatchNo());
        result.setExtractTime(cached.getExtractTime());
        result.setExperts(cached.getExperts());
        result.setIsFromCache(true);
        return result;
    }

    /**
     * 读取抽取结果缓存（先本地 Caffeine，再 Redis）。
     */
    private ExtractResultVO getCached(String cacheKey) {
        // L1：本地缓存
        ExtractResultVO local = localCache.getIfPresent(cacheKey);
        if (local != null) {
            return local;
        }
        // L2：Redis
        try {
            String json = stringRedisTemplate.opsForValue().get(EXTRACT_CACHE_PREFIX + cacheKey);
            if (json == null) {
                return null;
            }
            ExtractResultVO result = objectMapper.readValue(json, ExtractResultVO.class);
            // 回填本地缓存，下次直接命中 L1
            localCache.put(cacheKey, result);
            return result;
        } catch (Exception e) {
            // Redis 读取或反序列化失败，降级为仅本地缓存未命中
            log.warn("Redis 读取抽取缓存失败，降级为未命中，key: {}", EXTRACT_CACHE_PREFIX + cacheKey, e);
            return null;
        }
    }

    /**
     * 将抽取结果写入缓存（同时写本地 Caffeine 与 Redis）。
     */
    private void putCache(String cacheKey, ExtractResultVO result) {
        // L1：本地缓存（必定成功）
        localCache.put(cacheKey, result);
        // L2：Redis
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(EXTRACT_CACHE_PREFIX + cacheKey, json,
                    EXTRACT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis 写入失败仅影响分布式缓存，本地缓存仍生效
            log.warn("Redis 写入抽取缓存失败，仅本地缓存生效，key: {}", EXTRACT_CACHE_PREFIX + cacheKey, e);
        }
    }

    private ExtractExpertVO toExtractExpertVO(ExpertInfo expert, Map<String, String> applyTypeMap,
                                              Map<String, String> technicalTypeMap, Map<String, String> levelMap) {
        ExtractExpertVO vo = new ExtractExpertVO();
        vo.setId(expert.getId());
        vo.setName(expert.getName());
        vo.setApplyType(expert.getApplyType());
        vo.setApplyTypeLabel(applyTypeMap.get(expert.getApplyType()));
        vo.setTechnicalType(expert.getTechnicalType());
        vo.setTechnicalTypeLabel(technicalTypeMap.get(expert.getTechnicalType()));
        vo.setLevel(expert.getLevel());
        vo.setLevelLabel(levelMap.get(expert.getLevel()));
        vo.setPhone(expert.getPhone());
        vo.setCompany(expert.getCompany());
        return vo;
    }

    /**
     * 导出抽取专家结果为 Excel 字节数组。
     *
     * @param batchNo 抽取批次号
     * @return Excel 文件字节数组
     */
    @Override
    public byte[] exportExtractResult(String batchNo) {
        List<Map<String, Object>> experts = expertExtractRecordMapper.getExpertsByBatchNo(batchNo);
        Map<String, String> applyTypeMap = buildDictLabelMap("apply_type");
        Map<String, String> technicalTypeMap = buildDictLabelMap("technical_type");
        Map<String, String> levelMap = buildDictLabelMap("level");

        String[] headers = {"姓名", "申报类型", "技术类型", "级别", "联系方式", "工作单位"};
        List<List<String>> rows = new ArrayList<>();
        if (experts != null) {
            for (Map<String, Object> expert : experts) {
                String applyType = str(expert.get("applyType"));
                String technicalType = str(expert.get("technicalType"));
                String level = str(expert.get("level"));
                List<String> row = new ArrayList<>();
                row.add(str(expert.get("name")));
                row.add(applyTypeMap.getOrDefault(applyType, applyType));
                row.add(technicalTypeMap.getOrDefault(technicalType, technicalType));
                row.add(levelMap.getOrDefault(level, level));
                row.add(str(expert.get("phone")));
                row.add(str(expert.get("company")));
                rows.add(row);
            }
        }
        return ExcelUtil.create("抽取专家结果", headers, rows);
    }

    /**
     * 构建字典编码到中文标签的映射。
     */
    private Map<String, String> buildDictLabelMap(String dictType) {
        List<SysDict> dicts = sysDictMapper.getByType(dictType);
        Map<String, String> map = new HashMap<>();
        if (dicts != null) {
            for (SysDict dict : dicts) {
                map.put(dict.getDictCode(), dict.getDictValue());
            }
        }
        return map;
    }

    /**
     * 构建字典中文标签到编码的映射（label -> code），用于导入时把中文标签转回编码。
     */
    private Map<String, String> buildDictCodeMap(String dictType) {
        List<SysDict> dicts = sysDictMapper.getByType(dictType);
        Map<String, String> map = new HashMap<>();
        if (dicts != null) {
            for (SysDict dict : dicts) {
                map.put(dict.getDictValue(), dict.getDictCode());
            }
        }
        return map;
    }

    /**
     * 将中文标签转为编码；若已是编码或无法识别则原样返回。
     */
    private String toCode(String value, Map<String, String> labelToCodeMap) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        return labelToCodeMap.getOrDefault(value.trim(), value.trim());
    }

    /**
     * 构建专家 ID 到最近抽取时间的映射。
     */
    private Map<Long, LocalDateTime> buildLastExtractTimeMap(List<Long> expertIds) {
        Map<Long, LocalDateTime> map = new HashMap<>();
        List<Map<String, Object>> rows = expertExtractRecordMapper.getLastExtractTimeByExpertIds(expertIds);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object idObj = row.get("expertId");
                if (idObj == null) {
                    continue;
                }
                Long expertId = ((Number) idObj).longValue();
                LocalDateTime time = toLocalDateTime(row.get("lastExtractTime"));
                if (time != null) {
                    map.put(expertId, time);
                }
            }
        }
        return map;
    }

    /**
     * 将数据库时间值统一转为 LocalDateTime，兼容 LocalDateTime、Timestamp、Date 等类型。
     */
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }

    /**
     * 根据最近抽取时间计算抽取状态。
     */
    private String computeExtractStatus(LocalDateTime lastExtractTime) {
        if (lastExtractTime != null && lastExtractTime.isAfter(LocalDateTime.now().minusDays(30))) {
            return "已抽取";
        }
        return "未抽取";
    }

    /**
     * 校验专家必填字段，返回 null 表示合法，否则返回错误原因。
     */
    private String validateExpert(ExpertInfo expert) {
        if (expert.getName() == null || expert.getName().trim().isEmpty()) {
            return "姓名不能为空";
        }
        if (expert.getApplyType() == null || expert.getApplyType().trim().isEmpty()) {
            return "申报类型不能为空";
        }
        if (expert.getTechnicalType() == null || expert.getTechnicalType().trim().isEmpty()) {
            return "技术类型不能为空";
        }
        if (expert.getLevel() == null || expert.getLevel().trim().isEmpty()) {
            return "级别不能为空";
        }
        return null;
    }

    /**
     * 解析 Excel 文件为专家列表。
     */
    private List<ExpertInfo> parseExcel(MultipartFile file) {
        List<ExpertInfo> list = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                ExpertInfo expert = new ExpertInfo();
                expert.setName(getCellValue(row.getCell(0)));
                expert.setBirthday(parseDate(getCellValue(row.getCell(1))));
                expert.setEducation(getCellValue(row.getCell(2)));
                expert.setCompany(getCellValue(row.getCell(3)));
                expert.setApplyType(getCellValue(row.getCell(4)));
                expert.setTechnicalType(getCellValue(row.getCell(5)));
                expert.setLevel(getCellValue(row.getCell(6)));
                expert.setPhone(getCellValue(row.getCell(7)));
                list.add(expert);
            }
        } catch (Exception e) {
            throw new LoginFailedException("文件解析失败，请上传正确的 Excel 文件");
        }
        return list;
    }

    /**
     * 获取单元格字符串值，兼容日期与数字类型。
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return new BigDecimal(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 解析日期字符串。
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            try {
                return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("yyyy/M/d"));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * 将 null 列表转为空列表，避免返回 null。
     */
    private List<String> nullToEmpty(List<String> list) {
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 将对象转为字符串，null 转为空字符串。
     */
    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

}
