⚠️ 仍需关注问题 (8个)
High (3个)

1. RegPatternUtil.java - 缓存大小上限
   位置: RegPatternUtil.java:56

问题: Pattern 缓存没有自动淘汰机制

影响: 长期运行可能导致内存压力

建议:

方案A: 添加缓存大小上限，超出时使用 LRU 淘汰
方案B: 保持现状，依赖调用方主动调用 clearCache()
优先级: Medium

2. SnowIdGenerator.java - 字段可见性
   位置: SnowIdGenerator.java:127-132

问题: sequence 和 lastTimestamp 缺少 volatile 修饰

当前状态:

private long sequence = 0L; // 建议添加 volatile
private long lastTimestamp = -1L; // 建议添加 volatile
说明: 虽然 synchronized 方法提供了内存屏障，但在某些边缘情况下（如 JVM 重排序优化）可能存在风险。

优先级: Medium

3. HttpUtil.java - 中断处理不明确
   位置: HttpUtil.java:382-387

问题: 中断后返回的错误结果没有明确指示是中断导致

当前状态:

} catch (InterruptedException e) {
Thread.currentThread().interrupt();
break; // 跳出循环但结果仍然是错误
}
优先级: Low

Medium (4个)

4. FileUtil.java - ZIP炸弹风险
   位置: FileUtil.java:1044-1056

问题: ZIP 打包缺少单个文件大小限制

建议: 添加可选的文件大小检查参数

优先级: Low

5. FileUtil.java - 大文件流式处理
   位置: FileUtil.java:1301-1310

问题: PDF 整文件加载到内存

说明: 受限于 PDFBox 库，如需流式处理可能需要更换库

优先级: Low（设计权衡）

6. JsonUtil.java - 缓存监控
   位置: JsonUtil.java:107, 112

问题: SERIALIZER_REGISTRY 和 defaultSerializer 缓存缺少监控

优先级: Low

7. HttpRequestBuilder.java - URL验证
   位置: HttpRequestBuilder.java:74-77

问题: 缺少 URL 格式验证

说明: 构建时会自然报错，影响有限

优先级: Low

8. module-info.java (如果存在)
   问题: json.support 可能缺少 opens 声明

说明: 如果项目使用模块系统，需要确认

优先级: Low