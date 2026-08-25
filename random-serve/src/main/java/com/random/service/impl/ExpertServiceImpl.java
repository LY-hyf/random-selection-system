package com.random.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.random.constant.StatusConstant;
import com.random.context.BaseContext;
import com.random.dto.ExpertImportRow;
import com.random.exception.BaseException;
import com.random.exception.LoginFailedException;
import com.random.mapper.expert.ExpertExtractRecordMapper;
import com.random.mapper.expert.ExpertImportRecordMapper;
import com.random.mapper.expert.ExpertInfoMapper;
import com.random.mapper.dict.SysDictMapper;
import com.random.pojo.dto.ExpertPageRequest;
import com.random.pojo.dto.ExtractHistoryPageRequest;
import com.random.pojo.dto.ExtractRequest;
import com.random.pojo.entity.ExpertExtractRecord;
import com.random.pojo.entity.ExpertImportRecord;
import com.random.pojo.entity.ExpertInfo;
import com.random.pojo.entity.SysDict;
import com.random.pojo.vo.ExpertVO;
import com.random.pojo.vo.ExtractExpertVO;
import com.random.pojo.vo.ExtractResultVO;
import com.random.result.PageResult;
import com.random.service.ExpertService;
import com.random.utils.ExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    /** 注入自定义的专家导入线程池 */
    @Autowired
    private ThreadPoolExecutor expertImportPool;

    /** 任务状态 Key 前缀 */
    private static final String TASK_RESULT_PREFIX = "import:task:";

    /** 任务结果过期时间（10分钟，给前端充足时间轮询） */
    private static final long TASK_EXPIRE_MINUTES = 10;

    /** 专家导入批量插入大小 */
    private static final int IMPORT_BATCH_SIZE = 1000;

    /** 抽取锁，用于串行化抽取操作，防止并发重复抽取 */
    private final ReentrantLock extractLock = new ReentrantLock();

    /** 本地缓存（L1），用于快速命中，避免频繁访问 Redis */
    private final Cache<String, ExtractResultVO> localCache = Caffeine.newBuilder()
            .expireAfterWrite(EXTRACT_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 分页查询专家。
     *
     * <p>查询专家分页数据，并填充各字典编码对应的中文标签，最近抽取状态</p>
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
            // buildLastExtractTimeMap 构建专家 ID 到最近抽取时间的map集合映射（批量查询填充器）。
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
                // 查询专家抽取状态
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
     * 分页查询抽取历史记录。
     *
     * <p>该方法支持按条件分页检索专家的抽取历史，包括批次号、抽取时间、操作人等信息。
     * 查询结果不包含具体专家详情，仅展示抽取记录概览，适用于前端历史列表展示。</p>
     *
     * @param request 分页及筛选条件（包含页码、每页条数、批次号、时间范围等）
     * @return 抽取历史分页结果，包含总记录数及当前页数据
     */
    @Override
    public PageResult<Map<String, Object>> extractHistory(ExtractHistoryPageRequest request) {
        // 开启分页（PageHelper 会自动拦截后续 SQL）
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        // 执行查询，返回 List<Map>（每行记录包含 batchNo、expertName、extractTime 等字段）
        List<Map<String, Object>> list = expertExtractRecordMapper.pageQuery(request);
        // 封装分页信息
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
        // 记录操作日志（INFO 级别），便于追踪查询行为和性能
        log.info("抽取历史查询完成，pageNum: {}, pageSize: {}, 总记录数: {}",
                request.getPageNum(), request.getPageSize(), pageInfo.getTotal());
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
     * <p>采用 EasyExcel 流式读取（SAX 模式），逐行处理，内存友好，支持百万级数据。</p>
     * <p>导入流程：</p>
     * <ol>
     *   <li>校验上传文件是否为空，构建字典反向映射（中文标签 → 编码），兼容用户使用中文填写 Excel 的情况；</li>
     *   <li>初始化计数器（总数/成功数）和失败明细列表（线程安全），准备批量插入容器；</li>
     *   <li>注册 EasyExcel 读取监听器，逐行处理：</li>
     *   <ul>
     *     <li>跳过示例/占位行（名称包含“示例”或“请删除”）；</li>
     *     <li>增加总数计数；</li>
     *     <li>将 Excel 行数据转换为专家实体，对日期、字典字段做编码转换；</li>
     *     <li>进行业务校验（必填、格式等），失败则记录行号和原因，不进入数据库；</li>
     *     <li>校验通过后设置默认字段（状态、删除标记、创建时间），加入批量容器；</li>
     *     <li>当容器达到批量大小（IMPORT_BATCH_SIZE）时执行一次批量插入并清空容器；</li>
     *   </ul>
     *   <li>解析完成后，处理最后一批剩余数据；</li>
     *   <li>记录导入明细（文件名、总数、成功数、失败数、操作人），存入数据库；</li>
     *   <li>组装返回结果（总数、成功数、失败数、记录ID、失败明细列表）。</li>
     * </ol>
     *
     * @param file 上传的 Excel 文件（.xlsx 或 .xls）
     * @return 导入统计结果 Map，包含 totalCount / successCount / failCount / recordId / failDetails
     * @throws LoginFailedException 文件为空或解析失败时抛出
     */
    @Override
    public Map<String, Object> importExperts(MultipartFile file) {
        // 1. 文件基本校验
        if (file == null || file.isEmpty()) {
            throw new LoginFailedException("上传文件不能为空");
        }
        // 2. 构建字典反向映射（中文标签 -> 编码），用于将 Excel 中的中文值转换为数据库存储的编码
        //    例如：Excel 中填写“医疗” -> 转换为 “medical”
        //    同时兼容 Excel 中已填写编码（如 “medical”）的情况，映射会原样返回编码本身
        Map<String, String> applyTypeCodeMap = buildDictCodeMap("apply_type");
        Map<String, String> technicalTypeCodeMap = buildDictCodeMap("technical_type");
        Map<String, String> levelCodeMap = buildDictCodeMap("level");
        Map<String, String> educationCodeMap = buildDictCodeMap("education");
        // 3. 初始化统计变量（原子类确保线程安全）
        AtomicInteger totalCount = new AtomicInteger(0);        // 总读取行数（不含示例行）
        AtomicInteger successCount = new AtomicInteger(0);      // 成功导入数
        List<Map<String, Object>> failDetails = Collections.synchronizedList(new ArrayList<>()); // 失败明细（线程安全）
        List<ExpertInfo> batch = new ArrayList<>(IMPORT_BATCH_SIZE); // 批量插入容器
        // 4. 使用 try-with-resources 自动关闭输入流
        try (InputStream is = file.getInputStream()) {
            // 4.1 注册 EasyExcel 读取监听器（流式读，逐行回调）
            EasyExcel.read(is, ExpertImportRow.class, new ReadListener<ExpertImportRow>() {
                /**
                 * 每解析一行数据时回调。
                 *
                 * @param row     当前行数据（已映射到 ExpertImportRow）
                 * @param context 上下文（可获取行号等信息）
                 */
                @Override
                public void invoke(ExpertImportRow row, AnalysisContext context) {
                    // 跳过模板中的示例行（通常用于提示用户填写格式）
                    if (row.getName() != null
                            && (row.getName().contains("示例") || row.getName().contains("请删除"))) {
                        return;
                    }
                    // 4.1.1 增加总行数计数
                    totalCount.incrementAndGet();
                    // 4.1.2 创建专家实体，将 Excel 字段转换为数据库字段
                    ExpertInfo expert = new ExpertInfo();
                    expert.setName(row.getName());// 姓名
                    // parseDate // 出生日期（解析为 LocalDate）
                    expert.setBirthday(parseDate(row.getBirthday()));
                    expert.setEducation(toCode(row.getEducation(), educationCodeMap)); // 学历（中文→编码）
                    expert.setCompany(row.getCompany());                            // 工作单位
                    expert.setApplyType(toCode(row.getApplyType(), applyTypeCodeMap)); // 申报类型
                    expert.setTechnicalType(toCode(row.getTechnicalType(), technicalTypeCodeMap)); // 技术类型
                    expert.setLevel(toCode(row.getLevel(), levelCodeMap));          // 级别
                    expert.setPhone(row.getPhone());                                // 联系方式
                    // 4.1.3 获取当前行号（用于错误定位）
                    int rowNum = context.readRowHolder().getRowIndex() + 1;
                    // 4.1.4 校验专家数据（必填、格式、字典值有效性等）
                    String reason = validateExpert(expert);
                    if (reason != null) {
                        // 校验失败：记录失败明细（行号、姓名、失败原因）
                        Map<String, Object> fail = new HashMap<>();
                        fail.put("row", rowNum);
                        fail.put("name", expert.getName());
                        fail.put("reason", reason);
                        failDetails.add(fail);
                        return; // 跳过该行，不进行数据库操作
                    }
                    // 4.1.5 校验通过：设置默认字段
                    expert.setStatus(StatusConstant.ENABLE);   // 状态：启用
                    expert.setDeleted(0);                      // 未删除
                    expert.setCreateTime(LocalDateTime.now()); // 创建时间
                    // 4.1.6 加入批量插入容器
                    batch.add(expert);
                    // 4.1.7 当容器达到批量阈值时，执行一次批量插入并累加成功数
                    if (batch.size() >= IMPORT_BATCH_SIZE) {
                        successCount.addAndGet(flushImportBatch(batch));
                        // batch 在 flushImportBatch 内部会被清空（注意 flushImportBatch 需实现清空逻辑）
                    }
                }
                /**
                 * 所有数据解析完成后的回调（无论是否有异常）。
                 *
                 * @param context 上下文
                 */
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 4.2 处理最后一批剩余数据（不足 BATCH_SIZE 的部分）
                    successCount.addAndGet(flushImportBatch(batch));
                }
            }).sheet().doRead(); // 读取默认 sheet
        } catch (IOException e) {
            // 5. 文件 I/O 异常处理
            throw new LoginFailedException("文件解析失败，请上传正确的 Excel 文件");
        }
        // 6. 记录导入明细（用于历史追踪）
        ExpertImportRecord record = new ExpertImportRecord();
        record.setFileName(file.getOriginalFilename());
        record.setTotalCount(totalCount.get());
        record.setSuccessCount(successCount.get());
        record.setFailCount(totalCount.get() - successCount.get());
        record.setUserId(BaseContext.getCurrentId());          // 当前操作用户ID（从线程上下文获取）
        record.setCreateTime(LocalDateTime.now());
        expertImportRecordMapper.insert(record);               // 入库
        // 7. 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount.get());
        result.put("successCount", successCount.get());
        result.put("failCount", totalCount.get() - successCount.get());
        result.put("recordId", record.getId());                // 导入记录ID，供前端/后续查询
        result.put("failDetails", failDetails);                // 失败明细列表
        // 8. 记录业务日志
        log.info("专家导入完成，文件: {}, 总数: {}, 成功: {}, 失败: {}",
                file.getOriginalFilename(), totalCount.get(), successCount.get(),
                totalCount.get() - successCount.get());

        return result;
    }

    /**
     * 提交异步导入任务。
     *
     * <p>流程：
     * <ol>
     *   <li>生成唯一 taskId，存入 Redis 状态为 PROCESSING；</li>
     *   <li>使用 @Async 线程池异步执行已有的同步导入方法；</li>
     *   <li>立即返回 taskId，主线程不阻塞。</li>
     * </ol>
     *
     * @param file 上传的 Excel 文件
     * @return taskId，用于后续轮询查询结果
     */
    @Override
    public String submitImportAsyncTask(MultipartFile file) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        log.info("提交异步导入任务, taskId: {}, fileName: {}", taskId, file.getOriginalFilename());

        try {
            // ===== 关键修复：在提交任务前，将文件内容复制到内存 =====
            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();

            // 存入初始状态到 Redis
            try {
                Map<String, Object> pendingStatus = new HashMap<>();
                pendingStatus.put("status", "PROCESSING");
                pendingStatus.put("message", "任务已提交，正在后台导入...");
                pendingStatus.put("fileName", fileName);
                pendingStatus.put("createTime", LocalDateTime.now().toString());
                String json = objectMapper.writeValueAsString(pendingStatus);
                stringRedisTemplate.opsForValue().set(
                        TASK_RESULT_PREFIX + taskId,
                        json,
                        TASK_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (Exception e) {
                log.error("Redis 写入初始状态失败, taskId: {}", taskId, e);
                // 不抛出异常，让任务继续执行，但前端轮询时可能会查不到状态
            }

            // 异步执行导入（使用字节数组副本）
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("开始执行异步导入, taskId: {}, 线程: {}",
                            taskId, Thread.currentThread().getName());

                    // ===== 从字节数组重建 MultipartFile，解决异步的临时线程被回收问题=====
                    MultipartFile fileCopy = new MockMultipartFile(
                            fileName,
                            fileName,
                            contentType,
                            new ByteArrayInputStream(fileBytes)
                    );

                    // 调用同步导入方法
                    Map<String, Object> result = this.importExperts(fileCopy);

                    // 导入成功，更新 Redis 结果为 SUCCESS
                    result.put("status", "SUCCESS");
                    result.put("message", "导入完成");
                    String json = objectMapper.writeValueAsString(result);
                    stringRedisTemplate.opsForValue().set(
                            TASK_RESULT_PREFIX + taskId,
                            json,
                            TASK_EXPIRE_MINUTES,
                            TimeUnit.MINUTES
                    );
                    log.info("异步导入完成, taskId: {}, 成功: {}, 失败: {}",
                            taskId, result.get("successCount"), result.get("failCount"));

                } catch (Exception e) {
                    log.error("异步导入失败, taskId: {}", taskId, e);
                    try {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("status", "FAILED");
                        errorResult.put("message", "导入失败: " + e.getMessage());
                        errorResult.put("fileName", fileName);
                        String json = objectMapper.writeValueAsString(errorResult);
                        stringRedisTemplate.opsForValue().set(
                                TASK_RESULT_PREFIX + taskId,
                                json,
                                TASK_EXPIRE_MINUTES,
                                TimeUnit.MINUTES
                        );
                    } catch (Exception ex) {
                        log.error("Redis 写入失败状态异常, taskId: {}", taskId, ex);
                    }
                }
            // 使用定义的线程池
            }, expertImportPool);

            return taskId;
        } catch (IOException e) {
            log.error("读取文件内容失败, taskId: {}", taskId, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }

    /**
     * 查询导入任务结果。
     *
     * @param taskId 任务 ID
     * @return 任务状态和结果
     */
    @Override
    public Map<String, Object> getImportResult(String taskId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(TASK_RESULT_PREFIX + taskId);
            if (json == null) {
                Map<String, Object> notFound = new HashMap<>();
                notFound.put("status", "NOT_FOUND");
                notFound.put("message", "任务不存在或已过期");
                return notFound;
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("查询导入结果失败, taskId: {}", taskId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "查询失败: " + e.getMessage());
            return error;
        }
    }

    /**
     * 批量插入缓冲区的专家，返回插入条数。
     */
    private int flushImportBatch(List<ExpertInfo> batch) {
        int count = batch.size();
        if (count > 0) {
            expertInfoMapper.insertBatch(batch);
            batch.clear();
        }
        return count;
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
        // 获取当前登录用户ID（从线程上下文ThreadLocal中获取）
        Long userId = BaseContext.getCurrentId();
        // 构建缓存键，由“用户ID:申请类型:技术类型:级别”组成，确保不同条件组合的缓存隔离,确保每个用户5分钟缓存隔离
        String cacheKey = userId + ":" + request.getApplyType() + ":" + request.getTechnicalType() + ":" + request.getLevel();
        // ---- 第一阶段：无锁快速路径 ----
        // 先尝试从缓存中获取结果，若命中则直接返回（注意返回副本，避免缓存对象被修改）
        ExtractResultVO cached = getCached(cacheKey);
        if (cached != null) {
            log.info("抽取命中缓存，batchNo: {}", cached.getBatchNo());
            // 返回缓存数据的副本（深拷贝或浅拷贝视具体实现，此处方法名暗示返回不可变副本）
            return copyAsFromCache(cached);
        }
        // ---- 第二阶段：加锁防止并发重复抽取 ----
        // 使用可重入锁（ReentrantLock）保证同一时刻只有一个线程执行抽取逻辑，避免为相同条件生成多批次数据
        extractLock.lock();
        try {
            // 双重检查（Double-Check）：获取锁后再次查询缓存，防止在等待锁期间其他线程已完成抽取并写入缓存
            cached = getCached(cacheKey);
            if (cached != null) {
                log.info("抽取命中缓存（并发等待后），batchNo: {}", cached.getBatchNo());
                return copyAsFromCache(cached);
            }
            // ---- 第三阶段：数据库查询与随机筛选 ----
            // ===== 核心变更点：从 Redis Set 弹出 ID，替代 SQL 随机查询 =====
            // 旧逻辑（删除）：List<ExpertInfo> experts = expertInfoMapper.getExtractableExperts(...);
            // 新逻辑（替换）
            // 随机弹出1-20，默认为5的专家id
            List<Long>selectIds = popExpertIdsFromPool(
                    request.getApplyType(),
                    request.getTechnicalType(),
                    request.getLevel(),
                    request.getCount() == null ? 5 : Math.min(request.getCount(), 20)
            );
            // 批量根据专家id查询专家信息（主键索引）
            List<ExpertInfo>selected = expertInfoMapper.selectBatchIds(selectIds);
            // 第三阶段：记录抽取历史，保证30天抽取不重复
            String batchNo = "EX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            LocalDateTime now = LocalDateTime.now();
            for (ExpertInfo expert : selected){
                ExpertExtractRecord record = new ExpertExtractRecord();
                record.setBatchNo(batchNo);
                record.setExpertId(expert.getId());
                record.setUserId(userId);
                record.setApplyType(expert.getApplyType());
                record.setTechnicalType(expert.getTechnicalType());
                record.setLevel(expert.getLevel());
                record.setExtractTime(now);
                expertExtractRecordMapper.insert(record);
            }
            // ---- 第四阶段：构建字典标签映射（用于将编码转换为可读名称） ----
            // 分别获取申请类型、技术类型、级别的字典映射（如 "01" -> "初级"）
            Map<String, String> applyTypeMap = buildDictLabelMap("apply_type");
            Map<String, String> technicalTypeMap = buildDictLabelMap("technical_type");
            Map<String, String> levelMap = buildDictLabelMap("level");
            // ---- 第五阶段：组装返回结果对象 ----
            ExtractResultVO result = new ExtractResultVO();
            result.setBatchNo(batchNo);
            result.setExtractTime(now);
            result.setIsFromCache(false); // 本次为实时抽取，非缓存结果
            // 将专家实体转换为前端展示用的VO，并填充字典标签
            result.setExperts(selected.stream()
                    .map(e -> toExtractExpertVO(e, applyTypeMap, technicalTypeMap, levelMap))
                    .collect(Collectors.toList()));
            // ---- 第六阶段：缓存结果 ----
            // 将本次抽取结果放入缓存，后续相同条件的请求可直接复用（默认有过期时间，避免数据陈旧）
            putCache(cacheKey, result);
            log.info("抽取完成，batchNo: {}, 抽取人数: {}, 条件: applyType={}, technicalType={}, level={}",
                    batchNo, selected.size(), request.getApplyType(), request.getTechnicalType(), request.getLevel());
            return result;
        } finally {
            // 确保锁在任何情况下（包括异常）都能被释放，避免死锁
            extractLock.unlock();
        }
    }

    /**
     * 创建缓存结果对象的副本，并将其标记为来自缓存。
     * <p>
     * 该方法用于从缓存中获取结果后，生成一个新的 {@link ExtractResultVO} 实例，
     * 避免直接暴露缓存对象，防止外部修改影响缓存数据。复制时仅复制基本字段和引用，
     * 属于浅拷贝（专家列表仍引用原对象，但本业务场景下专家列表不会在返回后被修改）。
     *
     * @param cached 从缓存中获取的原始结果对象，不得为 {@code null}
     * @return 一个新的 {@link ExtractResultVO} 对象，其字段值与缓存对象一致，
     *         且 {@code isFromCache} 属性被设置为 {@code true}
     */
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
            log.info("本地缓存读取成功，key: {}", EXTRACT_CACHE_PREFIX + cacheKey);
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
            log.info("Redis 回填一级本地缓存，key: {}",EXTRACT_CACHE_PREFIX + cacheKey);
            return result;
        } catch (Exception e) {
            // Redis 读取或反序列化失败，降级为仅本地缓存未命中
            log.warn("Redis 读取缓存失败，降级为未命中，key: {}", EXTRACT_CACHE_PREFIX + cacheKey, e);
            return null;
        }
    }

    /**
     * 将抽取结果写入缓存（同时写本地 Caffeine 与 Redis）。
     */
    private void putCache(String cacheKey, ExtractResultVO result) {
        // L1：本地缓存（必定成功）
        localCache.put(cacheKey, result);
        log.info("本地缓存写入成功，key: {}", EXTRACT_CACHE_PREFIX + cacheKey);
        // L2：Redis
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(EXTRACT_CACHE_PREFIX + cacheKey, json,
                    EXTRACT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Redis 写入缓存成功，key: {}",EXTRACT_CACHE_PREFIX + cacheKey);
        } catch (Exception e) {
            // Redis 写入失败仅影响分布式缓存，本地缓存仍生效
            log.warn("Redis 写入缓存失败，仅本地缓存生效，key: {}", EXTRACT_CACHE_PREFIX + cacheKey, e);
        }
    }

    /**
     * 将专家实体转换为前端展示用的视图对象，并填充对应的字典标签（如类型名称、级别名称）。
     * <p>
     * 该方法用于将数据库查询得到的专家信息（编码形式）转换为前端需要的展示对象，
     * 通过传入的字典映射将编码转换为可读的中文标签，方便前端直接展示。
     *
     * @param expert            专家实体对象，包含编码字段（申请类型、技术类型、级别等）
     * @param applyTypeMap      申请类型字典映射，key为编码，value为标签名称
     * @param technicalTypeMap  技术类型字典映射，key为编码，value为标签名称
     * @param levelMap          级别字典映射，key为编码，value为标签名称
     * @return 填充了标签的 {@link ExtractExpertVO} 视图对象，包含专家所有展示字段及对应标签
     */
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
     * 构建专家 ID 到最近抽取时间的映射（批量查询填充器）。
     *
     * <p><b>业务目的：</b></p>
     * 在分页查询专家列表（`page` 方法）时，前端需要展示每个专家当前是“已抽取”还是“未抽取”状态。
     * 该状态依赖于该专家最近一次被抽取的时间是否在 30 天内。
     * 此方法用于批量获取这些时间数据，避免逐条查询数据库。
     *
     * <p><b>性能优化（防 N+1 查询）：</b></p>
     * 如果分页返回 10 条专家数据，循环调用 10 次 SQL 去查抽取时间会严重拖慢响应。
     * 该方法利用 SQL 的 `IN` 查询，一次性批量查出所有专家的最近抽取时间，
     * 将 10 次网络 IO 降为 1 次，极大提升列表页加载速度。
     *
     * @param expertIds 当前分页中所有的专家 ID 列表（调用方传入，不会为 null）
     * @return Map<专家ID, 最近一次抽取时间>。若无抽取记录，则 Map 中不包含该 ID
     */
    private Map<Long, LocalDateTime> buildLastExtractTimeMap(List<Long> expertIds) {
        Map<Long, LocalDateTime> map = new HashMap<>();

        // 防御性判断：如果列表为空，直接返回空 Map，避免无效的数据库查询
        if (expertIds == null || expertIds.isEmpty()) {
            return map;
        }

        // 1. 批量查询：调用 Mapper 层，SQL 大致逻辑为：
        //    SELECT expert_id, MAX(extract_time) as lastExtractTime
        //    FROM expert_extract_record
        //    WHERE expert_id IN (?, ?, ...)
        //    GROUP BY expert_id
        List<Map<String, Object>> rows = expertExtractRecordMapper.getLastExtractTimeByExpertIds(expertIds);

        if (rows != null) {
            for (Map<String, Object> row : rows) {
                // 2. 提取专家 ID（MyBatis 可能返回 Integer 或 Long，用 Number 接口统一接收）
                Object idObj = row.get("expertId");
                if (idObj == null) {
                    continue; // 数据异常行跳过
                }
                // 安全转换为 Long（如果是 Integer 也能自动转）
                Long expertId = ((Number) idObj).longValue();

                // 3. 转换时间（统一转为 LocalDateTime）
                //    兼容数据库返回的 java.sql.Timestamp、java.util.Date 等类型
                LocalDateTime time = toLocalDateTime(row.get("lastExtractTime"));
                if (time != null) {
                    map.put(expertId, time);
                }
            }
        }

        // 4. 返回映射关系，供上层计算 "已抽取/未抽取" 状态
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
     * 业务展示层，根据最近一次抽取时间计算当前抽取状态，并通过分页查询方法展示到前端。
     * <p>
     * 业务规则：若最近抽取时间在距今 30 天以内（包含当天），则认为当前处于“已抽取”状态；
     * 否则视为“未抽取”。该方法主要用于前端展示或业务逻辑判断，如控制抽取按钮的可操作性。
     *
     * @param lastExtractTime 最近一次抽取的时间，可为 {@code null}
     * @return 状态字符串：
     *         <ul>
     *             <li>若 {@code lastExtractTime} 不为 {@code null} 且在 30 天内 → {@code "已抽取"}</li>
     *             <li>否则 → {@code "未抽取"}</li>
     *         </ul>
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

    /**
     * 从 Redis Set 中弹出 N 个专家 ID（带自动补货逻辑）。
     */
    private List<Long> popExpertIdsFromPool(String applyType, String techType, String level, int count) {
        String poolKey = "pool:" + applyType + ":" + techType + ":" + level;
        // 1. 尝试直接弹出
        List<String> popped = stringRedisTemplate.opsForSet().pop(poolKey, count);
        if (popped != null && popped.size() >= count) {
            return popped.stream().map(Long::parseLong).collect(Collectors.toList());
        }
        // 2. 池子空了（或不够），需要补货（加分布式锁）
        String lockKey = poolKey + ":lock";
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 尝试获取锁，等待 5 秒，持有 30 秒
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                // 如果获取锁失败，直接抛出异常，避免后续无意义的 pop
                throw new BaseException("系统繁忙，请稍后重试");
            }
            // 双重检查：再次确认池子是否已被其他节点补满
            Long currentSize = stringRedisTemplate.opsForSet().size(poolKey);
            if (currentSize == null || currentSize < count) {
                // 查数据库（走复杂 NOT EXISTS 查询，只在首次或池耗尽时触发）
                List<Long> ids = expertInfoMapper.getExtractableExpertIds(applyType, techType, level);
                if (ids.isEmpty()) {
                    throw new BaseException("当前无符合条件的专家可抽取");
                }
                // 塞入 Redis Set
                String[] idArray = ids.stream().map(String::valueOf).toArray(String[]::new);
                stringRedisTemplate.opsForSet().add(poolKey, idArray);
                stringRedisTemplate.expire(poolKey, 30, TimeUnit.DAYS);
                log.info("补货专家池成功，key: {}, 数量: {}", poolKey, ids.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("系统繁忙，请重试");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
        // 3. 再次尝试弹出
        List<String> retryPopped = stringRedisTemplate.opsForSet().pop(poolKey, count);
        if (retryPopped == null || retryPopped.size() < count) {
            // 如果补货后仍然不足，说明数据库记录本身少于请求数量，抛出明确异常
            int actual = retryPopped == null ? 0 : retryPopped.size();
            throw new BaseException("符合条件的专家不足 " + count + " 人，当前仅 " + actual + " 人");
        }
        return retryPopped.stream().map(Long::parseLong).collect(Collectors.toList());
    }



}
