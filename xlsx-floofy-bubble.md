# 专家导入支持百万级数据（流式读取 + 批量插入）

## Context（背景）

当前 `ExpertServiceImpl.importExperts` 导入流程有两个瓶颈，无法支撑百万条数据：

1. **内存**：`parseExcel` 用 `WorkbookFactory.create(is)`（POI DOM 方式）把整个 .xlsx 一次性加载进内存，再组装成 `List<ExpertInfo>`。百万行会导致 OOM。
2. **性能**：循环里 `expertInfoMapper.insert(expert)` 逐条插入（每行一条 SQL、自动提交），百万次插入极慢甚至超时。

**目标**：改成「流式读取（SAX）+ 分批批量插入」，让导入百万级数据也能在可控内存与时间内完成。

## 方案（推荐 EasyExcel 流式读 + MyBatis 批量插）

### 1. 引入 EasyExcel 依赖

文件：`random-serve/pom.xml`

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>
</dependency>
```

> EasyExcel 基于 SAX 逐行解析，内存占用与行数无关，是 Java 大文件 Excel 的标准方案。

### 2. 新增批量插入 Mapper 方法

文件：`random-serve/src/main/java/com/random/mapper/expert/ExpertInfoMapper.java`

```java
@Insert("<script>" +
        "insert into expert_info (name, birthday, education, company, apply_type, technical_type, " +
        "level, phone, status, deleted, create_time) values " +
        "<foreach collection='list' item='e' separator=','>" +
        "(#{e.name}, #{e.birthday}, #{e.education}, #{e.company}, #{e.applyType}, #{e.technicalType}, " +
        "#{e.level}, #{e.phone}, #{e.status}, #{e.deleted}, #{e.createTime})" +
        "</foreach>" +
        "</script>")
int insertBatch(@Param("list") List<ExpertInfo> experts);
```

> 复用现有 `insert` 的字段映射，批量版本用 `<foreach>` 生成多 VALUES。

### 3. 重写 importExperts 为流式 + 批量

文件：`random-serve/src/main/java/com/random/service/impl/expert/ExpertServiceImpl.java`

- 移除 `parseExcel`（POI DOM 读取），改为 EasyExcel 流式读取。
- 用一个 `ReadListener<ExpertInfo>`（或自定义 DTO）：
  - `invoke(data, context)` 逐行回调：做「中文标签 → 编码」转换（复用 `buildDictCodeMap` + `toCode`）和必填校验（复用 `validateExpert`），把合法行暂存到 `batchList`。
  - `batchList` 攒满 N 条（如 1000）就调用 `expertInfoMapper.insertBatch(batchList)` 批量插入，然后清空。
  - 记录 `successCount` / `failDetails`。
- `doAfterAllAnalysed` 收尾：flush 最后一批、写导入记录（复用现有 `ExpertImportRecord` 逻辑）、返回统计结果。

关键复用（已有方法，不改）：
- `buildDictCodeMap(String)` / `toCode(String, Map)` —— 中文标签转编码（`ExpertServiceImpl` 内）。
- `validateExpert(ExpertInfo)` —— 必填校验。
- `ExpertImportRecordMapper.insert` —— 导入记录。

### 4. 字段映射（列顺序）

保持与模板一致（列序号 0~7）：姓名 / 出生年月 / 学历 / 工作单位 / 申报类型 / 技术类型 / 级别 / 联系方式。

用 EasyExcel 的 `@ExcelProperty(index = 0..7)` 映射到一个专用导入 DTO（`com.random.pojo.dto.expert.ExpertImportRow`，或复用 `ExpertInfo` 加注解），避免污染实体。

## 关键约束

- **批量大小**：1000~5000 条/批，避免单条 SQL 过大超过 MySQL `max_allowed_packet`。
- **事务**：可给 `importExperts` 加 `@Transactional`（或分批手动提交），大批量时建议「分批提交」而非单一长事务，防止长事务锁表/回滚代价大。
- **失败行**：流式模式下逐行校验，失败行记入 `failDetails`（含行号），不影响整体继续。

## 验证方式

1. 编译：`mvn compile -pl random-serve -am`
2. 单测：`mvn test -pl random-serve -am`（现有 12 用例应继续通过）
3. 手工联调：
   - 生成一份几万~几十万行的 .xlsx，调 `POST /api/experts/import`
   - 观察内存占用（JVM 堆应平稳，不再随行数线性暴涨）
   - 检查 `expert_info` 行数、导入记录 `successCount/failCount`
   - 验证中文标签/编码两种列内容都能正确转换落库
