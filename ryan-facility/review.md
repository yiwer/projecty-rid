Ryan-Facility Remaining Issues (Post-Partial-Fix)
🔴 Critical (8)
Issue 1

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/id/IdUtil.java:60-84
问题描述: getIdGenerator() 存在明显的竞态条件，当前实现采用双重检查锁定但未对缓存写入做足够的原子保护，存在并发重复赋值或读取旧值的风险。
当前状态: 经过最近实现，仍有潜在竞态风险（双重检查+临界区写入在高并发下可能产生可见性问题）。
影响: 在高并发场景下可能导致多次获取 Spring Bean、缓存错误的 SnowIdGenerator，影响分布式ID的一致性与性能。
修复建议:
将 getIdGenerator() 改为明显的“双重检查锁定 + volatile 缓存 + 细粒度锁”结构，或使用 AtomicReference 做 CAS 保护。
示例要点：在外层使用 CPU 友好的锁（类级别 synchronized 或一个专用锁对象），在内部再做缓存判断；缓存字段声明为
volatile/通过原子操作确保可见性。
Issue 2

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/id/support/SnowIdGenerator.java:127-132
问题描述: sequence 与 lastTimestamp 字段未声明为 volatile，单例方法 nextId() 是 synchronized，但字段可见性问题仍可能导致并发看到旧值。
当前状态: 仍存在可见性风险；当前实现仍以 synchronized 方法为主，但字段没有 volatile 标注。
影响: 可能出现序列重复、时间戳错乱等情况，尤其在多线程交错调用时。
修复建议:
将 sequence 与 lastTimestamp 声明为 volatile，必要时也将 initialized 声明为 volatile（或改为受控的原子性更新）。
评估是否需要将 nextId() 的锁粒度降低到局部锁或分段锁以提升并发性。
Issue 3

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/id/support/SnowIdGenerator.java:195
问题描述: nextId() 方法为方法级 synchronized，成为全局锁，且高并发场景下吞吐受限。
当前状态: 仍存在全局锁的性能瓶颈（若 sequence/lastTimestamp 已经是 volatile 的前提下，仍可考虑分段锁或无锁策略）。
影响: 高并发下的吞吐下降，潜在的阻塞风险。
修复建议:
引入分段锁、无锁队列/ CAS 操作，或使用 AtomicLong 组合时间戳/序列的无锁实现。
或实现按分片的数据中心/工作节点的 ID 生成以降低锁竞争。
Issue 4

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:387-392
问题描述: 路径穿越保护存在 TOCTOU 风险（检查完成后路径仍可能被劫持或发生变更）。
当前状态: 已有先验的 startsWith 检查，但未使用 toRealPath(NO FOLLOW LINKS) 进行最终校验。
影响: 安全性受损，存在潜在文件覆盖、路径篡改等风险。
修复建议:
将目标路径解析为真实路径并用 NOFOLLOW_LINKS 的 real path 进行对比，防止中间路径被修改，
或在写入前直接使用 Files.createTempFile 之类的安全 API 进行写入。
Issue 5

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:1284-1292
问题描述: 将 PDF 全部读取到内存进行解析，易引发大文件内存压力。
当前状态: 仍保留完全加载字节数组再解析的模式（如 Loader.loadPDF(bytes)）。
影响: 大文件场景可能造成 OOM。
修复建议:
尽量改用流式/分块读取，或在入口实现文件大小限制；
如果使用第三方库支持流式解析，优先采用流式读取模式。
Issue 6

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:1018-1043
问题描述: zipFiles 中对每个文件的 IOException 未逐文件捕捉，若某个文件出错会导致整个 ZIP 打包失败。
当前状态: 仍然沿用外层统一 try-catch 的处理方式，缺少逐文件的容错。
影响: 部分文件出错时整个打包过程失败，影响稳定性。
修复建议:
为每个文件处理增加独立的 try-catch，遇到错误单独记录并跳过该文件继续打包；
增加对单个文件错误的日志记录和返回结果的详细错误信息。
Issue 7

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:1298-1326
问题描述: extractTextFromOfdStream 对 temp 文件的删除在删除失败时静默忽略，存在磁盘临时文件积压风险。
当前状态: 删除失败时未日志记录，可能导致磁盘空间长期积累。
影响: 影射运行时的磁盘空间压力，长期运行风险较高。
修复建议:
在 finally 块对删除失败进行日志输出（WARN 级别），并考虑使用 deleteOnExit 作为补充；
如库允许，改为使用显式清理策略（例如在调用方显式清理、或通过引用计数机制触发清理）。
Issue 8

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:459-479
问题描述: toTempFile() 返回的临时文件未给出清理策略，存在潜在的临时文件堆积。
当前状态: 现有变更中引入了 deleteOnExit，仍需确保调用方对清理有明确约束（最好提供 deleteTempFile() 辅助方法）。
影响: 可能导致磁盘垃圾，尤其在高并发/高文件上传场景。
修复建议:
保留 deleteOnExit，同时建议返回一个可显式清理的对象或 API，明确调用方清理职责；
文档中标注临时文件的清理责任。
Issue 9

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:182-238
问题描述: detectMimeType(InputStream) 会消费输入流，导致调用方无法复用流。
当前状态: 该行为未统一对外暴露为“消费型 API”的显式约定。
影响: 调用方若需再次使用流会失败或收到异常。
修复建议:
在方法文档中明确“InputStream 会被消费”的约定；
如需复用，请在调用前将输入流缓存为可重复读取的流（如用 ByteArrayOutputStream 先读出改为字节数组再传入；
或提供支持 mark/reset 的输入流前提下的实现。
Issue 10

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/json/support/InputStreamDeserializer.java:45-46
问题描述: 当 Base64 传入非法字符串时，解码会抛出 IllegalArgumentException，但未捕获处理。
当前状态: 当前实现直接抛出异常。
影响: 运行期异常未被捕获，可能导致 JSON 反序列化失败并崩溃。
修复建议:
捕获 IllegalArgumentException 并返回 Optional.empty()，或将异常转换为 WrappedError 进行统一错误返回。
Issue 11

文件与定位: ryan-facility/src/main/java/com/github/ryan/facility/json/support/InputStreamSerializer.java:40-47
问题描述: InputStream 在序列化时通过 readAllBytes() 读取，若流较大可能耗尽内存；文档中应标注流不可复用。
当前状态: 序列化实现仍然读取整流，可能导致内存压力。
影响: 大流量场景下可能引发 OOM。
修复建议:
评估是否能改为分块读取并写入 Base64，或使用可缓存的输出流；
在 API 文档中明确流不可重复使用的前提。
Issue 12

文件与定位: ryan-facility/module-info.java
问题描述: 可能缺少对 json.support 模块的 opens 声明，影响反射/动态代理。
当前状态: 未在现有代码片段中确认 opens 声明是否覆盖 json.support。
影响: 运行时反射/Spring 相关加载可能受限。
修复建议:
如使用 Java 模块系统，添加：opens com.github.ryan.facility.json.support to spring.core, spring.beans, spring.context;
注：以上“Critical (8)
”列出的问题均为当前仓库中仍可明确定位并具有较高风险的待修点。若某些项在你最近提交的变更中已经以设计权衡或已正确实现形式得到缓解，欢迎你标注为“已修复/后续设计已实现”，以便我在此处仅列出尚未落地的残留项。

🟠 High (15)
Issue A

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:387-392
描述: 路径穿越 TOCTOU 风险，需改用 toRealPath(NO FOLLOW LINKS) 进行最终校验
建议修复: 引入对最终路径的 real path 检查，确保不存在符号链接/路径劫持。
Issue B

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:1284-1292
描述: 大文件内存加载到内存，可能导致 OOM
建议: 使用流式解析或对输入大小进行上限。
Issue C

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:1018-1043
描述: zipFiles 对单文件 IO 异常缺乏逐文件容错
建议: 为每个文件增加独立的 try-catch，失败文件记录日志并跳过。
Issue D

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/pattern/RegPatternUtil.java:56, 131-133
描述: Pattern 缓存无上限，可能导致内存压力
建议: 加入容量上限或 LRU 淘汰策略。
Issue E

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/http/client/model/HttpClientConfig.java:227-231
描述: Basic Auth 处理涉及明文凭据，内存暴露风险
建议: 使用 char[] 保存凭据，必要时清理，避免日志/栈中暴露。
Issue F

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/http/client/model/HttpRequestBuilder.java:74-77
描述: 构造阶段缺少对 baseUrl 的格式验证
建议: 增加对 URL 的校验，防止非法 URL 导致 LaterError。
Issue G

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/http/client/HttpUtil.java:376-390
描述: 重试循环中对中断处理，返回的错误不明确指示中断
建议: 将中断情况下的返回改为专门的中断错误/异常，或抛出自定义中断异常。
Issue H

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/json/support/InputStreamDeserializer.java:45-46
描述: 无效 Base64 时未捕获
建议: 捕获 IllegalArgumentException，返回 Optional.empty 或错误信息。
Issue I

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/json/support/InputStreamSerializer.java:40-47
描述: 流被完全消费，可能导致后续消费失败
建议: 记录并文档化此行为，或考虑改用可重复读取的输入流。
Issue J

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/date/DateUtil.java:63
描述: FORMATTER_MAP 可能随用户模式增长，已知潜在内存膨胀
建议: 对模式缓存进行上限控制或定期清理。
Issue K

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/log/LogUtil.java:197-206
描述: CAS 竞态条件下对新 Handler 的替换可能不生效，需要更严格的双重检查
建议: 在 CAS 成功后再从缓存读取，确保使用最新的处理器。
Issue L

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/file/FileUtil.java:1260-1278
描述: 废弃方法缺少 forRemoval 及 since 版本信息
建议: 标记为 @Deprecated(since = "...", forRemoval = true)
Issue M

文件/定位: ryan-facility/src/main/java/com/github/ryan/facility/common/CommonUtil.java:550-551
描述: 对 value2 的 null 检查前未判空，理论上仍可能 NPE；虽在现实现中被 guard 但需谨慎
建议: 统一空指针防御策略，避免潜在的 NPE。
Issue N

文件/定位: ryan-facility/module-info.java（若存在 opens 声明缺失）
描述: json.support 未被 opens 声明，反射场景可能失败
建议: 打开 json.support 模块对外暴露。