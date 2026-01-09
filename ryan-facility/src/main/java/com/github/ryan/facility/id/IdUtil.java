package com.github.ryan.facility.id;


import com.github.ryan.facility.context.SpringContextHolder;
import com.github.ryan.facility.id.support.SnowIdGenerator;

import java.util.UUID;

/**
 * <b>ID生成工具类</b>
 * <p>
 * 提供多种 ID 生成策略，包括雪花算法ID和UUID。
 * 雪花算法ID支持分布式环境，可通过Spring配置注入自定义的{@link SnowIdGenerator}。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 生成雪花ID
 * Long id = IdUtil.snowId();
 *
 * // 生成UUID
 * UUID uuid = IdUtil.uuid();
 * String uuidStr = IdUtil.uuidStr();
 *
 * // 解析雪花ID
 * long timestamp = IdUtil.parseTimestamp(id);
 * String info = IdUtil.parseInfo(id);
 * }</pre>
 *
 * @author yvvb
 * @since 2025/4/20
 */
public final class IdUtil {

    /**
     * 私有构造函数，防止实例化
     */
    private IdUtil() {
    }

    /**
     * 默认的雪花ID生成器（dataCenterId=0, workerId=0）
     */
    private static final SnowIdGenerator DEFAULT_SNOW_ID_GENERATOR = new SnowIdGenerator(0, 0);

    /**
     * 缓存的ID生成器引用
     */
    private static volatile SnowIdGenerator cachedGenerator = null;

    /**
     * <b>获取ID生成器</b>
     * <p>
     * 优先使用Spring容器中的Bean，否则使用默认生成器。
     * 支持延迟获取，确保Spring容器初始化后能正确获取Bean。
     * </p>
     *
     * @return ID生成器实例
     */
    private static SnowIdGenerator getIdGenerator() {
        // 本地变量避免多次读取volatile字段
        SnowIdGenerator cached = cachedGenerator;
        if (cached != null && cached != DEFAULT_SNOW_ID_GENERATOR) {
            return cached;
        }

        // 双重检查锁定模式
        synchronized (IdUtil.class) {
            cached = cachedGenerator;
            if (cached != null && cached != DEFAULT_SNOW_ID_GENERATOR) {
                return cached;
            }

            // 尝试从Spring容器获取
            SnowIdGenerator springBean = SpringContextHolder.getBean(SnowIdGenerator.class).orElse(null);
            if (springBean != null) {
                cachedGenerator = springBean;
                return springBean;
            }
        }

        // 如果Spring容器未就绪，使用默认生成器（不缓存，下次调用再尝试获取）
        return DEFAULT_SNOW_ID_GENERATOR;
    }

    /**
     * <b>生成雪花算法ID</b>
     * <p>
     * 生成的ID具有以下特性：
     * </p>
     * <ul>
     *     <li>全局唯一</li>
     *     <li>趋势递增</li>
     *     <li>支持分布式环境</li>
     * </ul>
     *
     * @return 雪花算法生成的唯一ID
     */
    public static Long snowId() {
        return getIdGenerator().nextId();
    }

    /**
     * <b>生成UUID对象</b>
     *
     * @return {@link UUID} 随机生成的UUID
     */
    public static UUID uuid() {
        return UUID.randomUUID();
    }

    /**
     * <b>生成UUID字符串</b>
     * <p>
     * 格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     * </p>
     *
     * @return UUID字符串
     */
    public static String uuidStr() {
        return UUID.randomUUID().toString();
    }

    /**
     * <b>生成不含连字符的UUID字符串</b>
     * <p>
     * 格式：xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx (32位)
     * </p>
     *
     * @return 不含连字符的UUID字符串
     */
    public static String uuidSimpleStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ==================== ID 解析方法 ====================

    /**
     * <b>从雪花ID中解析时间戳</b>
     *
     * @param id 雪花ID
     * @return 生成该ID时的时间戳（毫秒）
     */
    public static long parseTimestamp(long id) {
        return SnowIdGenerator.parseTimestamp(id);
    }

    /**
     * <b>从雪花ID中解析工作节点ID</b>
     *
     * @param id 雪花ID
     * @return 工作节点ID
     */
    public static long parseWorkerId(long id) {
        return SnowIdGenerator.parseWorkerId(id);
    }

    /**
     * <b>从雪花ID中解析数据中心ID</b>
     *
     * @param id 雪花ID
     * @return 数据中心ID
     */
    public static long parseDataCenterId(long id) {
        return SnowIdGenerator.parseDataCenterId(id);
    }

    /**
     * <b>从雪花ID中解析序列号</b>
     *
     * @param id 雪花ID
     * @return 序列号
     */
    public static long parseSequence(long id) {
        return SnowIdGenerator.parseSequence(id);
    }

    /**
     * <b>获取雪花ID的详细信息（用于调试）</b>
     *
     * @param id 雪花ID
     * @return 包含各部分信息的字符串
     */
    public static String parseInfo(long id) {
        return SnowIdGenerator.parseInfo(id);
    }
}