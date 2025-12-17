# Projecty-Rid

> 一个轻量级、高性能的 Java 工具库集合，提供企业级应用开发的基础设施支持。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17+-orange.svg)](https://www.oracle.com/java/)

## 📖 项目简介

Projecty-Rid 是一个模块化的 Java 工具库项目，旨在为企业级应用开发提供高质量、易用的基础组件。项目采用现代 Java 特性，提供类型安全、函数式编程友好的 API 设计。

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### Maven 依赖

```xml
<!-- 基础工具模块 -->
<dependency>
    <groupId>cn.hbads.ryan</groupId>
    <artifactId>ryan-facility</artifactId>
    <version>dev</version>
</dependency>

<!-- 版本管理模块 -->
<dependency>
    <groupId>cn.hbads.ryan</groupId>
    <artifactId>ryan-version</artifactId>
    <version>dev</version>
</dependency>
```

## 📦 模块说明

### Ryan-Facility 模块

核心工具模块，提供日常开发中常用的工具类和基础设施支持。

### Ryan-Version 模块

版本管理模块，提供基于日期的数据版本链管理功能，支持版本的新增、修改、删除、移动和懒加载。

## 🛠️ 核心功能

### 1. JSON 工具 (`JsonUtil`)

基于 Jackson 的 JSON 序列化/反序列化工具，设计灵感来自 Rust 的 serde 库。

**核心特性：**
- ✅ Result 驱动的错误处理
- ✅ 完整的泛型类型支持
- ✅ 多序列化策略（default/generic/canonical/pretty）
- ✅ 链式 API 调用

**使用示例：**

```java
// 序列化
Result<String, WrappedError> result = JsonUtil.serialize(user);
String json = result.orElse("{}");

// 反序列化
Result<User, WrappedError> userResult = JsonUtil.deserialize(json, User.class);

// 泛型类型反序列化
Result<List<User>, WrappedError> listResult = 
    JsonUtil.deserialize(json, new TypeReference<List<User>>() {});

// 链式处理
String name = JsonUtil.deserialize(json, User.class)
    .map(User::getName)
    .orElse("unknown");

// 使用特定序列化器
JsonUtil.use(JsonUtil.PRETTY).serialize(object);
```

**内置序列化器：**
- `DEFAULT` - 标准配置，适合大多数场景
- `GENERIC` - 通用配置，使用标准 Java 时间模块
- `CANONICAL` - 规范化配置，属性按字母序排列
- `PRETTY` - 美化输出配置，便于调试

---

### 2. 日期时间工具 (`DateUtil`)

提供日期时间的格式化、解析、比较、转换等常用操作。

**核心特性：**
- ✅ 内置格式化器缓存
- ✅ 多种日期格式自动解析
- ✅ 空安全的日期比较
- ✅ Date 与 LocalDate/LocalDateTime 互转

**使用示例：**

```java
// 格式化日期
Result<String, WrappedError> result = 
    DateUtil.format(LocalDate.now(), "yyyy-MM-dd");

// 解析日期字符串（支持多种格式）
Result<LocalDate, WrappedError> dateResult = 
    DateUtil.parseDate("2025-01-01", "yyyy-MM-dd");

// 日期比较
boolean isBefore = DateUtil.isBefore(date1, date2);
boolean isSameDay = DateUtil.isSameDay(date1, date2);

// 日期转换
Date date = DateUtil.localDateToDate(LocalDate.now());
LocalDate localDate = DateUtil.dateToLocalDate(new Date());

// 安全的日期操作
LocalDate tomorrow = DateUtil.tomorrow(LocalDate.now());
LocalDate yesterday = DateUtil.yesterday(LocalDate.now());

// 获取较早/较晚日期
LocalDate min = DateUtil.minOne(date1, date2);
LocalDate max = DateUtil.maxOne(date1, date2);
```

**常用常量：**
- `MAX_DATE` - 业务最大日期 (9999-12-31)
- `MIN_DATE` - 业务最小日期 (1000-01-01)
- `SUPPORT_DATE_FORMAT` - 支持的日期格式数组

---

### 3. ID 生成工具 (`IdUtil`)

提供多种 ID 生成策略。

**核心特性：**
- ✅ 雪花算法 ID（分布式唯一）
- ✅ UUID 生成
- ✅ Spring 容器集成
- ✅ ID 解析功能

**使用示例：**

```java
// 生成雪花 ID
Long id = IdUtil.snowId();

// 生成 UUID
UUID uuid = IdUtil.uuid();
String uuidStr = IdUtil.uuidStr();
String simpleUuid = IdUtil.uuidSimpleStr(); // 无连字符

// 解析雪花 ID
long timestamp = IdUtil.parseTimestamp(id);
long workerId = IdUtil.parseWorkerId(id);
long dataCenterId = IdUtil.parseDataCenterId(id);
String info = IdUtil.parseInfo(id);
```

---

### 4. HTTP 客户端工具 (`HttpUtil`)

对 Java HttpClient 的优雅封装，设计灵感来自 Rust 的 reqwest 库。

**核心特性：**
- ✅ 流畅的链式 API
- ✅ Result 驱动的错误处理
- ✅ 同步/异步请求支持
- ✅ 自动重试机制
- ✅ JSON/Form/文本请求体

**使用示例：**

```java
// 简单 GET 请求
Result<HttpResult, WrappedError> result = 
    HttpUtil.get("https://api.example.com/users")
        .query("page", 1)
        .send();

// POST JSON 请求
Result<HttpResult, WrappedError> result = 
    HttpUtil.post("https://api.example.com/users")
        .json(user)
        .bearerAuth(token)
        .send();

// 链式处理响应
User user = HttpUtil.get("https://api.example.com/users/1")
    .send()
    .flatMap(HttpResult::requireSuccess)
    .flatMap(r -> r.json(User.class))
    .orElse(null);

// 异步请求
CompletableFuture<Result<HttpResult, WrappedError>> future = 
    HttpUtil.get(url).sendAsync();

// 带重试的请求
HttpUtil.get(url)
    .retry(3, Duration.ofSeconds(1))
    .send();

// 自定义客户端
HttpClient client = HttpClientConfig.create()
    .connectTimeout(Duration.ofSeconds(30))
    .proxy("proxy.example.com", 8080)
    .build();
HttpUtil.withClient(client).get(url).send();
```

---

### 5. 文件工具 (`FileUtil`)

提供文件上传、下载、类型检测、哈希计算等功能。

**核心特性：**
- ✅ 基于魔数的文件类型检测（Apache Tika）
- ✅ 路径穿越安全检查
- ✅ 文件名清洗
- ✅ 中文文件名支持
- ✅ MD5/SHA-256 哈希

**使用示例：**

```java
// 安全保存上传文件
Result<Path, WrappedError> result = 
    FileUtil.saveFile(multipartFile, "/upload")
        .peek(path -> log.info("保存成功: {}", path));

// 文件类型检测（基于魔数）
boolean isImage = FileUtil.isImage(file);
String mimeType = FileUtil.detectMimeType(file).orElse("application/octet-stream");

// 文件下载
FileUtil.download(response, file, "下载文件名.pdf");

// 文件哈希
Result<String, WrappedError> md5 = FileUtil.md5(file);
Result<String, WrappedError> sha256 = FileUtil.sha256(file);

// 文件名处理
String uniqueName = FileUtil.generateUniqueFileName("原始文件名.jpg");
String datePath = FileUtil.generateDatePath(); // 2025/12/17

// 类型校验保存
FileUtil.saveFileWithTypeCheck(
    file, 
    "/upload", 
    Set.of("image/jpeg", "image/png")
);
```

---

### 6. 日志工具 (`LogUtil`)

统一的日志输出工具，自动获取调用者类名。

**核心特性：**
- ✅ 自动获取调用者类名
- ✅ 弱引用缓存 Logger
- ✅ 支持日志后处理器
- ✅ 占位符格式化

**使用示例：**

```java
// 基本使用
LogUtil.info("用户{}登录成功", username);
LogUtil.error(exception, "处理订单{}失败", orderId);

// 各级别日志
LogUtil.trace("追踪信息: {}", data);
LogUtil.debug("调试信息: {}", data);
LogUtil.warn("警告信息: {}", data);
LogUtil.warn(exception, "警告: {}", message);

// 动态设置日志级别
LogUtil.setLevel(MyClass.class, Level.DEBUG);
```

---

### 7. Result 类型

函数式错误处理类型，设计灵感来自 Rust 的 Result<T, E>。

**核心特性：**
- ✅ 不可变性
- ✅ 空安全
- ✅ 链式操作
- ✅ 函数式组合

**使用示例：**

```java
// 创建 Result
Result<Integer, String> success = Result.ok(42);
Result<Integer, String> failure = Result.err("计算失败");

// 链式处理
String message = success
    .map(n -> n * 2)
    .ensure(n -> n > 50, () -> "数值太小")
    .map(Object::toString)
    .orElse("默认值");

// 异常捕获
Result<String, Exception> result = Result.of(() -> riskyOperation())
    .peekErr(e -> logger.error("操作失败", e))
    .recover(e -> "fallback");

// 收集多个结果
List<Result<Integer, String>> results = ...;
Result<List<Integer>, String> combined = results.stream()
    .collect(Result.toResult());

// 模式匹配
result.match(
    value -> System.out.println("成功: " + value),
    error -> System.err.println("失败: " + error)
);
```

---

### 8. 对象拷贝工具 (`CopyUtil`)

提供集合的深拷贝功能。

**使用示例：**

```java
// List 深拷贝
List<User> copiedList = CopyUtil.copyList(originalList);

// Set 深拷贝
Set<User> copiedSet = CopyUtil.copySet(originalSet);

// Map 值深拷贝
Map<String, User> copiedMap = CopyUtil.copyMapValues(originalMap);

// Map 键值全拷贝
Map<Key, Value> copiedMap = CopyUtil.copyMapAll(originalMap);
```

---

### 9. 其他工具

**CommonUtil** - 通用工具
```java
// 集合判空
boolean isEmpty = CommonUtil.isEmpty(collection);

// 容量计算
int capacity = CommonUtil.calculateCapacity(size);
```

**NumberUtil** - 数字工具
```java
// 数字处理
```

**LocaleUtil** - 国际化工具
```java
// 国际化处理
```

**RegPatternUtil** - 正则工具
```java
// 正则匹配
boolean matches = RegPatternUtil.matches(pattern, text);
```

**SpringContextHolder** - Spring 上下文工具
```java
// 获取 Bean
Result<MyService, WrappedError> result = 
    SpringContextHolder.getBean(MyService.class);
```

---

### 10. 版本管理 (`Version Module`)

基于日期的数据版本链管理系统，支持版本分裂、懒加载等高级特性。

#### 10.1 解决的问题

在企业应用中，许多业务数据需要支持**历史版本追溯**和**预约变更**功能，例如：

| 场景 | 问题描述 |
|------|----------|
| 公交站点管理 | 站点名称从2025-06-01起变更，需要保留历史名称以供查询 |
| 线路规划 | 线路将在未来某日调整，需要预先配置并在指定日期自动生效 |
| 票价调整 | 票价分阶段调整，每个时间段对应不同的票价 |
| 合同管理 | 合同条款在不同时期有不同版本，需要按生效日期查询 |

**传统解决方案的痛点：**

```
❌ 方案一：每次变更新增一条记录
   - 查询时需要复杂的日期过滤逻辑
   - 无法直观看到数据的完整生命周期
   - 数据一致性难以保证

❌ 方案二：使用时间戳字段
   - 历史版本查询性能差
   - 版本之间的连续性难以维护
   - 删除操作难以处理
```

**Version Module 的解决方案：**

```
✅ 版本链模型：将同一数据的所有版本组织成有序链表
✅ 自动分裂：修改时自动处理版本边界
✅ 懒加载：按需加载业务数据，减少内存占用
✅ 变更追踪：自动标记变更的版本，简化持久化
```

#### 10.2 数据结构设计

**版本链模型：**

```
┌─────────────────────────────────────────────────────────────────┐
│                        VersionChain                             │
│  dataId: 1001, dataType: STOP                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│   │ Version1 │───▶│ Version2 │───▶│ Version3 │                  │
│   │ CREATE   │    │ MODIFY   │    │ DELETE   │                  │
│   │ 01-01    │    │ 06-01    │    │ 12-01    │                  │
│   │ ~ 05-31  │    │ ~ 11-30  │    │ ~ 12-01  │                  │
│   └──────────┘    └──────────┘    └──────────┘                  │
│                                                                 │
│   versionDateTreeMap:    [01-01]→V1, [06-01]→V2, [12-01]→V3    │
│   expirationDateTreeMap: [05-31]→V1, [11-30]→V2, [12-01]→V3    │
└─────────────────────────────────────────────────────────────────┘
```

**核心数据结构：**

| 类 | 职责 |
|----|------|
| `BusinessData<D>` | 业务数据接口，定义数据标识、深拷贝、修改能力 |
| `ReleaseDateVersionData` | 版本元数据（dataId, dataType, versionDate, expirationDate, type） |
| `AbstractReleaseDateVersion<D,V>` | 版本抽象基类，包装业务数据和版本元数据 |
| `ReleaseDateVersionChain<D,V>` | 版本链接口，定义版本操作规范 |
| `AbstractReleaseDateVersionChain<D,V>` | 版本链实现，使用双 TreeMap 索引 |

**版本类型状态机：**

```
                    ┌─────────────┐
                    │  NO_CHANGE  │ (从数据库加载的未修改版本)
                    └──────┬──────┘
                           │ modify/delete
              ┌────────────┴────────────┐
              ▼                         ▼
        ┌──────────┐              ┌──────────┐
        │  MODIFY  │              │  DELETE  │
        └────┬─────┘              └──────────┘
             │ modify/delete            ▲
             └──────────────────────────┘

        ┌──────────┐
        │  CREATE  │ (新创建的版本)
        └────┬─────┘
             │ delete (当天删除则清除整个链)
             ▼
        ┌──────────┐
        │  DELETE  │
        └──────────┘
```

#### 10.3 版本操作详解

**1. 修改操作 (modify)**

```java
// 场景：站点在 2025-06-01 改名
versionChain.modify(LocalDate.of(2025, 6, 1), new StopModifyModel("新站名"));
```

```
情况A：修改日期 = 版本生效日期（合并）
┌────────────────────────────┐       ┌────────────────────────────┐
│ Version [01-01 ~ 12-31]    │  ──▶  │ Version [01-01 ~ 12-31]    │
│ name = "旧站名"            │       │ name = "新站名"            │
│ type = CREATE              │       │ type = CREATE (保持不变)   │
└────────────────────────────┘       └────────────────────────────┘

情况B：修改日期 在版本有效期内（分裂）
┌────────────────────────────┐       ┌────────────┐ ┌──────────────┐
│ Version [01-01 ~ 12-31]    │  ──▶  │ V1 [01-01  │ │ V2 [06-01    │
│ name = "旧站名"            │       │  ~ 05-31]  │ │  ~ 12-31]    │
│ type = CREATE              │       │ name=旧    │ │ name=新      │
└────────────────────────────┘       │ type=CREATE│ │ type=MODIFY  │
                                     └────────────┘ └──────────────┘
```

**2. 删除操作 (delete)**

```java
// 场景：站点在 2025-12-01 停用
versionChain.delete(LocalDate.of(2025, 12, 1));
```

```
情况A：删除创始版本（清除整个链）
┌────────────────────────────┐
│ Version [01-01 ~ 12-31]    │  ──▶  (空链，所有版本移至 removedVersions)
└────────────────────────────┘

情况B：删除非创始日期（分裂出删除版本）
┌────────────────────────────┐       ┌────────────┐ ┌──────────────┐
│ Version [01-01 ~ 12-31]    │  ──▶  │ V1 [01-01  │ │ V2 [12-01]   │
│ type = CREATE              │       │  ~ 11-30]  │ │ type=DELETE  │
└────────────────────────────┘       │ type=CREATE│ │ deleted=true │
                                     └────────────┘ └──────────────┘
```

**3. 移动操作 (move)**

```java
// 场景：将版本生效日期从 06-01 调整为 07-01
versionChain.move(version, LocalDate.of(2025, 7, 1));
```

```
约束：移动不能跨越其他版本

┌────────────┐ ┌────────────┐     ┌────────────┐ ┌────────────┐
│ V1 [01-01  │ │ V2 [06-01  │ ──▶ │ V1 [01-01  │ │ V2 [07-01  │
│  ~ 05-31]  │ │  ~ 12-31]  │     │  ~ 06-30]  │ │  ~ 12-31]  │
└────────────┘ └────────────┘     └────────────┘ └────────────┘
                  移动到 07-01       前版本过期日期自动调整
```

**4. 查询操作**

```java
// 查询某日期生效的版本
Optional<V> version = chain.findEffectiveVersion(LocalDate.of(2025, 8, 15));

// 查询与日期范围交叉的所有版本
Collection<V> versions = chain.getVersionsByRangeCross(
    LocalDate.of(2025, 3, 1),
    LocalDate.of(2025, 9, 1)
);

// 获取创始日期和删除日期
LocalDate genesisDate = chain.getGenesisDate();
Optional<LocalDate> deletedDate = chain.getDeletedDate();
```

#### 10.4 懒加载机制

**问题场景：**

```
一条线路有 100 个版本，但用户只查询 2025-08-01 的数据。

❌ 全量加载：从数据库加载 100 个版本的业务数据 → 内存浪费、查询慢
✅ 懒加载：只加载需要的版本 → 按需加载、高效
```

**Window-N 懒加载策略：**

```
查询 2025-08-01 生效的版本，窗口大小 N=2

版本链： V1[01-01] ─ V2[04-01] ─ V3[06-01] ─ V4[08-01] ─ V5[10-01] ─ V6[12-01]
                              ◄───────┬───────►
                                   Window-2
                              加载: V3, V4, V5

策略优势：
- 预加载相邻版本，减少后续查询的数据库访问
- 批量加载，减少数据库往返次数
- 只加载必要数据，控制内存占用
```

**实现懒加载器：**

```java
public class StopVersionLazyLoader 
        extends WindowNStrategyReleaseDateVersionLazyLoader<StopData> {
    
    private final StopRepository repository;
    
    public StopVersionLazyLoader(
            LazyLoadableReleaseDateVersionChain<StopData> chain,
            StopRepository repository) {
        super(chain, 2);  // 窗口大小 N=2，加载前后各2个版本
        this.repository = repository;
    }
    
    @Override
    public void batchLoadVersions(Long dataId, Set<ReleaseDateVersionData> identities) {
        // 1. 从数据库批量查询
        Set<LocalDate> versionDates = identities.stream()
            .map(ReleaseDateVersionData::getVersionDate)
            .collect(Collectors.toSet());
        List<StopEntity> entities = repository.findByDataIdAndVersionDates(dataId, versionDates);
        
        // 2. 转换为业务数据映射
        Map<ReleaseDateVersionData, StopData> dataMap = entities.stream()
            .collect(Collectors.toMap(
                e -> findIdentity(identities, e.getVersionDate()),
                StopData::fromEntity
            ));
        
        // 3. 加载到版本链
        versionChain.loadAggregatedDataToChain(dataMap);
    }
}
```

**懒加载版本链的使用：**

```java
// 创建懒加载版本链
LazyLoadableReleaseDateVersionChain<StopData> chain = new StopVersionChain(dataId, dataType, versions);

// 设置懒加载器
StopVersionLazyLoader loader = new StopVersionLazyLoader(chain, repository);
versions.forEach(v -> v.setVersionLoader(loader));

// 访问业务数据时自动触发加载
StopData data = chain.findEffectiveVersion(LocalDate.now())
    .map(LazyLoadableReleaseDateVersion::getBusinessData)  // 触发懒加载
    .orElse(null);
```

#### 10.5 持久化集成

版本链提供变更追踪，简化持久化逻辑：

```java
public void saveVersionChain(ReleaseDateVersionChain<StopData, ?> chain) {
    // 1. 保存/更新变更的版本
    Collection<? extends AbstractReleaseDateVersion<StopData, ?>> changedVersions = 
        chain.getChangedVersions();
    for (var version : changedVersions) {
        if (version.getReleaseVersionType() == ReleaseVersionType.CREATE) {
            repository.insert(toEntity(version));
        } else {
            repository.update(toEntity(version));
        }
    }
    
    // 2. 删除已移除的版本
    List<AloneReleaseDateVersion<StopData>> removedVersions = chain.getRemovedVersions();
    for (var removed : removedVersions) {
        repository.deleteByDataIdAndVersionDate(
            removed.getDataId(),
            removed.getVersionDate()
        );
    }
}
```

#### 10.6 Spring 事件集成

```java
// 定义版本链事件
public class StopVersionChainEvent 
        extends ReleaseDateVersionChainEvent<StopData, LazyLoadableReleaseDateVersion<StopData>, StopVersionChain> {
    
    public StopVersionChainEvent(StopVersionChain chain) {
        super(chain);
    }
}

// 发布事件
@Service
public class StopVersionService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void modifyStop(Long dataId, LocalDate modifyDate, StopModifyModel model) {
        StopVersionChain chain = loadChain(dataId);
        chain.modify(modifyDate, model);
        saveVersionChain(chain);
        
        // 发布变更事件
        eventPublisher.publishEvent(new StopVersionChainEvent(chain));
    }
}

// 监听事件
@Component
public class StopVersionEventListener {
    @EventListener
    public void onVersionChainChanged(StopVersionChainEvent event) {
        StopVersionChain chain = event.getVersionChain();
        log.info("站点 {} 版本链变更", chain.getDataId());
        // 触发缓存刷新、通知等后续处理
    }
}
```

#### 10.7 完整使用示例

```java
// 1. 定义业务数据
@Data
public class StopData implements BusinessData<StopData> {
    private Long stopId;
    private String stopName;
    private String address;
    private Double longitude;
    private Double latitude;
    
    @Override
    public Long getDataId() { return stopId; }
    
    @Override
    public Integer getDataType() { return DataType.STOP.getCode(); }
    
    @Override
    public StopData copy() {
        StopData copy = new StopData();
        BeanUtils.copyProperties(this, copy);
        return copy;
    }
    
    @Override
    public void modifyData(BusinessDataModifyInterface model) {
        if (model instanceof StopModifyModel m) {
            Optional.ofNullable(m.getStopName()).ifPresent(v -> this.stopName = v);
            Optional.ofNullable(m.getAddress()).ifPresent(v -> this.address = v);
            Optional.ofNullable(m.getLongitude()).ifPresent(v -> this.longitude = v);
            Optional.ofNullable(m.getLatitude()).ifPresent(v -> this.latitude = v);
        }
    }
}

// 2. 定义修改模型
@Data
public class StopModifyModel implements BusinessDataModifyInterface {
    private String stopName;
    private String address;
    private Double longitude;
    private Double latitude;
}

// 3. 使用版本链
@Service
@RequiredArgsConstructor
public class StopVersionService {
    private final StopVersionChainFactory factory;
    private final StopRepository repository;
    
    // 创建站点（创建版本链）
    public void createStop(StopCreateModel model) {
        StopVersionChain chain = factory.createDataVersionChain(model);
        saveVersionChain(chain);
    }
    
    // 修改站点（版本分裂）
    public Result<Void, WrappedError> modifyStop(Long stopId, LocalDate effectiveDate, StopModifyModel model) {
        StopVersionChain chain = loadChain(stopId);
        Result<Void, WrappedError> result = chain.modify(effectiveDate, model);
        if (result.isOk()) {
            saveVersionChain(chain);
        }
        return result;
    }
    
    // 查询某日期的站点数据
    public Optional<StopData> getStopAt(Long stopId, LocalDate queryDate) {
        StopVersionChain chain = loadChain(stopId);
        return chain.findEffectiveVersion(queryDate)
            .filter(v -> !v.isDeleted())
            .map(AbstractReleaseDateVersion::getBusinessData);
    }
}
```

---

## 🎨 设计理念

### 1. Result 驱动的错误处理

所有可能失败的操作都返回 `Result<T, E>` 类型，强制调用方处理错误，避免隐式异常。

```java
// ❌ 传统方式
try {
    String json = objectMapper.writeValueAsString(obj);
} catch (JsonProcessingException e) {
    // 容易被忽略的错误处理
}

// ✅ Result 方式
Result<String, WrappedError> result = JsonUtil.serialize(obj);
result.ifErr(err -> log.error("序列化失败", err.getException()));
```

### 2. 函数式编程友好

提供 map、flatMap、filter、recover 等链式操作。

```java
String userName = getUserById(userId)
    .flatMap(user -> user.getProfile())
    .map(Profile::getName)
    .filter(name -> name.length() > 0, () -> "名字为空")
    .orElse("未知用户");
```

### 3. 零依赖异常

工具类不抛出受检异常，所有错误封装到 Result 中。

### 4. 安全优先

- 文件操作：路径穿越检查、文件名清洗
- JSON 操作：严格的类型检查
- HTTP 请求：自动超时、重试机制

---

## 📚 技术栈

### 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Jackson | Latest | JSON 序列化 |
| Apache Tika | Latest | 文件类型检测 |
| Hutool Core | Latest | 基础工具 |
| Commons Lang3 | Latest | 字符串/日期工具 |
| SLF4J + Logback | Latest | 日志框架 |
| Spring Boot | Latest | 自动配置 |

### 可选依赖

- `spring-web` - HTTP 工具、文件下载
- `jakarta.validation-api` - 参数校验

---

## 🔧 配置说明

### 雪花 ID 生成器配置

```java
@Configuration
public class IdConfig {
    @Bean
    public SnowIdGenerator snowIdGenerator() {
        return new SnowIdGenerator(
            dataCenterId,  // 数据中心 ID (0-31)
            workerId       // 工作节点 ID (0-31)
        );
    }
}
```

### 日志后处理器配置

```java
@Component
public class MyLogPostHandler implements LogPostHandler {
    @Override
    public void handle(LogContext context) {
        // 自定义日志处理逻辑（如上报）
    }
}
```

---

## 📊 性能优化

1. **缓存机制**
   - DateTimeFormatter 缓存
   - Logger 实例弱引用缓存
   - 日志后处理器缓存

2. **延迟初始化**
   - 序列化器双重检查锁定
   - Spring Bean 延迟获取

3. **内存优化**
   - 弱引用缓存避免内存泄漏
   - 流式处理大文件

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范

1. 代码风格：遵循 Java 编码规范
2. 文档：所有公共 API 必须有 JavaDoc
3. 测试：核心功能需要单元测试
4. 提交：遵循 Conventional Commits 规范

---

## 📄 许可证

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 作者

**Ryan (yvvb)**
- GitHub: [@yiwer](https://github.com/yiwer)

---

## 🙏 致谢

设计灵感来源：
- Rust 标准库 (Result, Option)
- Rust Serde (JSON 序列化)
- Rust Reqwest (HTTP 客户端)

---

**Last Updated:** 2025-12-17