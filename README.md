# 朔州市随机抽取系统

一个基于 Spring Boot 的专家随机抽取系统：管理专家库，按条件（申报类型 + 技术类型 + 级别）随机抽取专家，配套用户/角色/权限（RBAC）、首页大屏、数据字典、操作日志、Excel 导入导出等功能。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [核心功能](#核心功能)
- [权限设计（RBAC）](#权限设计rbac)
- [接口概览](#接口概览)
- [关键设计点](#关键设计点)
- [测试](#测试)

---

## 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 2.7.18 | 基础框架 |
| Java | 1.8 | 语言 |
| MyBatis | 2.3.2 | ORM（注解 + XML 混合） |
| PageHelper | 1.4.7 | 分页 |
| MySQL | 8.x | 数据库 |
| Redis | — | 分布式缓存（L2） |
| Caffeine | — | 本地缓存（L1） |
| Spring Security + JWT | — | 认证鉴权（RBAC） |
| Apache POI | 5.2.3 | Excel 导入导出 |
| Lombok | — | 简化 POJO |

---

## 项目结构

### 模块结构（Maven 多模块）

| 模块 | 职责 | 关键内容 |
|---|---|---|
| `random-common` | 公共组件 | `Result`/`PageResult`、异常体系、常量、`JwtUtil`、`JwtProperties`、`BaseContext`、`@Log` 注解 |
| `random-pojo` | POJO | `entity`（实体）、`dto`（入参）、`vo`（出参），按模块分 `auth`/`user`/`role`/`expert` 等子包 |
| `random-serve` | 主应用 | `controller`/`service`/`mapper`/`config`/`security`/`aspect`/`handler`，启动类 `RandomServeApplication` |

### 包结构（分层 + 按模块分子包）

```
com.random
├── controller/{auth,user,role,permission,expert,dashboard,dict,log}
├── service/{...}  +  service/impl/{...}
├── mapper/{user,role,permission,expert,dashboard,dict,log}
├── config/{SecurityConfig, WebMvcConfig}
├── security/{JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler}
├── handler/GlobalExceptionHandler
├── aspect/OperationLogAspect
└── json/JacksonObjectMapper

com.random.pojo
├── entity/{user,role,permission,expert,dict,log}
├── dto/{auth,user,role,permission,expert,log}
└── vo/{auth,user,role,permission,expert}
```

---

## 快速开始

### 环境要求

- JDK 1.8
- Maven 3.6+
- MySQL 8.x
- Redis（可选，用于分布式缓存；不启动则降级为仅本地缓存）

### 步骤

1. **初始化数据库**

   依次执行项目根目录下的 SQL 脚本：

   ```bash
   # 建表（结构）
   shuozhou_random_extract.sql
   # 初始数据（角色、权限、字典、专家、用户等）
   初始数据.sql
   ```

2. **修改配置**

   编辑 `random-serve/src/main/resources/application.yml`，改数据库连接（账号/密码）与 Redis 地址。

3. **编译打包**

   ```bash
   mvn install -DskipTests
   ```

4. **启动**

   ```bash
   # 方式一：Maven 启动
   mvn spring-boot:run -pl random-serve

   # 方式二：运行 jar
   java -jar random-serve/target/random-serve-0.0.1-SNAPSHOT.jar
   ```

5. **访问**

   服务地址：`http://localhost:8080/api`（默认 context-path 为 `/api`）

   默认管理员账号：`admin` / `123456`

---

## 配置说明

主要配置见 `random-serve/src/main/resources/application.yml`：

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `spring.datasource.*` | MySQL 连接 | `localhost:3306/shuozhou_random_extract` |
| `spring.redis.*` | Redis 连接 | `localhost:6379` |
| `mybatis.mapper-locations` | Mapper XML 位置 | `classpath:mapper/**/*.xml` |
| `mybatis.configuration.map-underscore-to-camel-case` | 驼峰映射 | `true` |
| `server.port` | 服务端口 | `8080` |
| `server.servlet.context-path` | 接口前缀 | `/api` |
| `jwt.admin-secret-key` | JWT 签名密钥 | `random-system-secret` |
| `jwt.admin-ttl` | Token 有效期（毫秒） | `7200000`（2 小时） |
| `jwt.admin-token-name` | Token 请求头名 | `Authorization` |

> ⚠️ 生产环境请务必修改数据库密码与 JWT 签名密钥。

---

## 核心功能

| 模块 | 说明 |
|---|---|
| 认证 | 登录/注册/登出/当前用户，JWT 签发与校验 |
| RBAC | 用户 → 角色 → 权限（多对多），后端按**权限编码**鉴权 |
| 专家管理 | 增删改查、Excel 导入（中文标签自动转编码）、导出、抽取历史 |
| 随机抽取（核心） | 按「申报类型 AND 技术类型 AND 级别」交集 + 近 30 天去重 → 结果集内随机 → 取 N 位（默认 5，上限 20） |
| 首页大屏 | 统计、技术/级别/申报类型分布（字典转中文）、最新抽取记录（按批次分组）、导出 |
| 数据字典 | 编码 ↔ 中文标签，全系统统一转中文 |
| 操作日志 | AOP 自动记录登录/增删改/抽取/导入等操作 |

---

## 权限设计（RBAC）

### 数据模型

```
用户(User) ──多对多──> 角色(Role) ──多对多──> 权限(Permission)
   sys_user           sys_role           sys_permission
   sys_user_role       sys_role_permission
```

- **角色**：一组权限的集合，代表一个岗位/身份（如「系统管理员」「普通用户」）。
- **权限**：最小访问控制单位，有「菜单/按钮/接口」三种类型，通过 `permission_code`（如 `user:add`、`expert:extract`）标识。

### 如何分配权限

在系统界面的「角色管理 → 为角色分配权限」中，给某个角色勾选对应的权限（`sys_role_permission` 表记录关联关系）。

给「普通用户」角色分配「专家抽取」权限的等价 SQL：

```sql
INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.id, p.id, NOW()
FROM sys_role r, sys_permission p
WHERE r.role_code = 'USER' AND p.permission_code = 'expert:extract'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
```

### 如何禁用权限

有两种方式，效果都是让用户失去该权限：

1. **停用权限**：权限管理里把某条权限的 `status` 改为 `0`（停用）。
2. **从角色移除**：角色管理里取消勾选该权限（删除 `sys_role_permission` 关联）。

### `@PreAuthorize("hasAuthority('expert:add:submit')")` 有什么用

这是 **Spring Security 方法级权限校验**。作用是：

- 在调用 Controller 方法**之前**，校验当前登录用户是否拥有 `expert:add:submit` 这个权限。
- 拥有 → 正常执行；不拥有 → 抛 `AccessDeniedException` → 由 `GlobalExceptionHandler` 返回 `{"code":0, "msg":"无权限访问该功能"}`。

```java
@PreAuthorize("hasAuthority('expert:add:submit')")  // 校验是否有「新增专家」权限
@PostMapping
public Result add(@Valid @RequestBody ExpertInfo expert) { ... }
```

完整鉴权链路：

```
请求 → JwtAuthenticationFilter（解析 token，加载「角色 + 权限」到 SecurityContext）
     → @PreAuthorize（校验方法所需权限）
     → Controller 方法
```

> 关键点：JWT 过滤器加载的是**权限编码**（`getPermissionCodesByUserId`，已过滤 `status=1`）。所以禁用某个权限后，该权限码不再加载，`@PreAuthorize` 校验即失败 → 返回「无权限」。

---

## 接口概览

统一前缀 `/api`，返回格式 `{code, msg, data}`（`code=1` 成功，`code=0` 失败）。

| 模块 | 路径 | 方法 | 说明 |
|---|---|---|---|
| 认证 | `/auth/login` | POST | 登录，返回 token + 用户信息 |
| 认证 | `/auth/register` | POST | 注册 |
| 认证 | `/auth/logout` | POST | 登出 |
| 认证 | `/auth/current-user` | GET | 当前用户信息 |
| 用户 | `/users/page` | GET | 分页查询用户 |
| 用户 | `/users` | POST | 新增用户 |
| 用户 | `/users/{id}` | PUT/DELETE | 编辑/删除用户 |
| 用户 | `/users/{id}/reset-password` | PUT | 重置密码 |
| 用户 | `/users/{id}/roles` | GET/PUT | 获取/分配角色 |
| 角色 | `/roles/page`、`/roles/all` | GET | 分页/全量角色 |
| 角色 | `/roles` | POST | 新增角色 |
| 角色 | `/roles/{id}` | PUT/DELETE | 编辑/删除角色 |
| 角色 | `/roles/{id}/permissions` | GET/PUT | 获取/分配权限 |
| 权限 | `/permissions/page`、`/tree`、`/menu` | GET | 分页/权限树/菜单 |
| 权限 | `/permissions` | POST | 新增权限 |
| 权限 | `/permissions/{id}` | PUT/DELETE | 编辑/删除权限 |
| 专家 | `/experts/page` | GET | 分页查询专家 |
| 专家 | `/experts/filter-options` | GET | 筛选下拉 |
| 专家 | `/experts/extract` | POST | 随机抽取 |
| 专家 | `/experts/extract-history` | GET | 抽取历史 |
| 专家 | `/experts/export` | GET | 导出抽取结果 |
| 专家 | `/experts` | POST | 新增专家 |
| 专家 | `/experts/{id}` | GET/PUT/DELETE | 详情/编辑/删除 |
| 专家 | `/experts/import` | POST | Excel 导入 |
| 专家 | `/experts/import/template` | GET | 下载导入模板 |
| 专家 | `/experts/import-records` | GET | 导入历史 |
| 大屏 | `/dashboard/statistics` | GET | 统计汇总 |
| 大屏 | `/dashboard/*-distribution` | GET | 各类分布 |
| 大屏 | `/dashboard/latest-extracts` | GET | 最新抽取记录 |
| 大屏 | `/dashboard/latest-extracts/export` | GET | 导出最新记录 |
| 字典 | `/dicts/type/{dictType}`、`/dicts/all` | GET | 查询字典 |
| 字典 | `/dicts` | POST | 新增字典 |
| 字典 | `/dicts/{id}` | PUT/DELETE | 编辑/删除字典 |
| 日志 | `/logs/page` | GET | 分页查询日志 |
| 日志 | `/logs/clear` | DELETE | 清空日志 |

---

## 关键设计点

1. **认证**：`JwtAuthenticationFilter` 解析 Bearer token，加载「角色 + 权限」作为 `SimpleGrantedAuthority`，写入 `SecurityContext` 和 `BaseContext`（ThreadLocal，`finally` 清理防泄漏）。

2. **鉴权**：`@PreAuthorize("hasAuthority('权限编码')")`，禁用权限后 `getPermissionCodesByUserId` 不再返回 → 返回「无权限」。

3. **多级缓存**：抽取结果缓存 L1=Caffeine（5min）+ L2=Redis（5min），读「L1→L2→库」，写「同时写两级」，Redis 挂掉降级为仅 L1。

4. **并发**：抽取用 `ReentrantLock` + 双重检查，防并发重复抽取。

5. **异常**：`GlobalExceptionHandler` 统一处理业务异常、参数校验、类型错误、`AccessDeniedException`（无权限）、重复键等。

6. **数据模型**：**实体存编码 + 字典编码转中文** —— `expert_info` 存 `medical`，字典映射 `medical→医疗`，前端展示中文。Excel 导入时自动把中文标签转回编码。

7. **输入校验**：Bean Validation（`@NotBlank`/`@Size`/`@Pattern`）+ `@Valid`，登录只做非空校验，注册/新增做完整格式校验（用户名长度、密码长度、手机号格式）。

---

## 测试

```bash
# 运行单元测试（随机抽取 + 登录校验）
mvn test -pl random-serve -am
```

现有测试覆盖：

- `AuthServiceImplTest`：登录成功/账号不存在/密码错误/账号禁用、注册、当前用户。
- `ExpertServiceImplTest`：抽取数量、结果集为空、缓存命中。

---

## 数据流示例（随机抽取）

```
POST /api/experts/extract {applyType, technicalType, level, count}
  → JwtAuthenticationFilter（鉴权）
  → ExpertController（@PreAuthorize 权限校验）
  → ExpertService.extract
      1. 查缓存 L1(Caffeine) → L2(Redis)
      2. 未命中 → 加锁 → 查可抽取专家（AND 交集 + 30 天去重）
      3. shuffle 随机 → 取前 N 位
      4. 落库 expert_extract_record
      5. 写缓存 L1+L2
  → 返回 {batchNo, extractTime, isFromCache, experts}
```

---

## 面试技术亮点（可写进简历/面试讲）

> 每个点按「难点 → 方案 → 效果」整理，方便面试时讲清楚。

### 1. RBAC 权限模型 + 方法级鉴权

- **难点**：不同用户能访问哪些功能？禁用某个权限后要立即生效。
- **方案**：三级模型 `用户 → 角色 → 权限`（多对多，`sys_user_role` / `sys_role_permission` 两张关联表）。后端用 Spring Security 的 `@PreAuthorize("hasAuthority('权限编码')")` 做方法级鉴权，而不是按角色硬编码。
- **效果**：权限配置化，禁用某权限后 `getPermissionCodesByUserId`（已过滤 `status=1`）不再返回 → 鉴权失败 → 返回「无权限」。做到了**细粒度、可配置、即时生效**。

### 2. 多级缓存（本地 Caffeine + 分布式 Redis）

- **难点**：抽取结果既要「快」又要「多实例一致」。
- **方案**：L1 用 Caffeine（JVM 内存，纳秒级），L2 用 Redis（分布式共享）。读「L1 → L2 → 库」并回填 L1；写「同时写两级」。
- **效果**：单实例命中走 L1 最快；跨实例走 L2 一致；**Redis 挂掉自动降级为仅 L1**，不影响主流程。

### 3. 并发防重（抽取）

- **难点**：并发请求可能抽到同一批专家（重复抽取）。
- **方案**：`ReentrantLock` + **双重检查锁（DCL）**：先查缓存（无锁快速路径）→ 加锁 → 再查缓存 → 才真正抽取。抽取结果落库 `expert_extract_record` 作为 30 天去重依据。
- **效果**：单实例内串行化抽取，避免并发重复。

### 4. 百万级 Excel 导入优化

- **难点**：原来用 POI 整表加载 + 逐条 insert，百万行会 **OOM** 且极慢。
- **方案**：改用 **EasyExcel 流式读取（SAX 逐行解析，内存与行数无关）** + **MyBatis 批量插入（1000 条/批）**。
- **效果**：内存恒定、插入批量化，可支撑百万级数据导入。

### 5. JWT 无状态认证 + ThreadLocal 防泄漏

- **难点**：无状态登录态如何传递当前用户？如何避免串号？
- **方案**：`JwtAuthenticationFilter` 解析 Bearer token，把「角色 + 权限」写入 `SecurityContext`，同时把 userId 存进 `BaseContext`（ThreadLocal），**并在 `finally` 里 `remove()` 清理**。
- **效果**：无状态、可水平扩展；ThreadLocal 及时清理，避免线程复用导致的用户 ID 串号/内存泄漏。

### 6. 数据规范化（实体存编码 + 字典转中文）

- **难点**：类型字段（申报类型/技术类型/级别/学历）既要能筛选，又要展示中文。
- **方案**：实体表存**编码**（如 `medical`），字典表存「编码 → 中文」（`medical → 医疗`），展示时用字典转中文；Excel 导入时自动把中文标签转回编码。
- **效果**：数据规范、可维护、可扩展，避免中英文混存。

### 7. 全局异常处理 + 参数校验

- **方案**：`GlobalExceptionHandler`（`@RestControllerAdvice`）统一处理业务异常、参数校验失败、类型错误、`AccessDeniedException`（无权限）、唯一键冲突等；入参用 Bean Validation（`@NotBlank`/`@Size`/`@Pattern`）+ `@Valid`。
- **效果**：接口返回格式统一 `{code, msg, data}`，错误提示友好。

### 8. AOP 操作日志

- **方案**：自定义 `@Log(module, operation)` 注解 + `@Around` 切面，自动记录操作人、模块、操作内容、请求地址、IP、结果，落 `sys_operation_log`。
- **效果**：业务代码零侵入，日志自动采集，便于审计。

### 9. 多模块 Maven 项目 + 分层分包

- **结构**：`random-common`（公共组件）/ `random-pojo`（实体、DTO、VO）/ `random-serve`（主应用）三模块；`controller / service / mapper / dto / vo / entity` 骨架层下再按业务模块分 `auth / user / role / expert ...` 子包。
- **效果**：职责清晰、模块解耦、可复用、易维护。

---

## 常见面试追问（简要答）

| 追问 | 回答要点 |
|---|---|
| `@PreAuthorize` 和拦截器鉴权有什么区别？ | `@PreAuthorize` 是 Spring Security 方法级鉴权（AOP），在方法执行前校验权限，注解声明式、粒度细；拦截器是 Web 层拦截，粒度粗。 |
| Caffeine 和 Redis 为什么用两级？ | Caffeine 快（本地内存）但不跨实例共享；Redis 能共享但慢（网络往返）。两级兼顾速度与一致性。 |
| 缓存一致性怎么保证？ | 写时双写 L1+L2；读时 L1 未命中回源 L2 并回填 L1；Redis 失效时降级 L1。 |
| 抽取怎么去重？ | 落库 `expert_extract_record` 记录抽取时间，SQL 用 `not exists` 排除近 30 天已抽取的专家。 |
| 为什么实体存编码不直接存中文？ | 编码稳定、可枚举、便于筛选和字典翻译，前端展示时再转中文，避免中英文混存和翻译耦合。 |
| EasyExcel 为什么比 POI 更适合大文件？ | EasyExcel 基于 SAX 逐行解析（内存恒定），POI 的 XSSFWorkbook 是 DOM 方式整表加载（内存随行数线性增长）。 |
| 分页怎么做的？ | PageHelper 拦截 SQL 自动加 LIMIT，`PageResult` 统一返回 `total + records`。 |

