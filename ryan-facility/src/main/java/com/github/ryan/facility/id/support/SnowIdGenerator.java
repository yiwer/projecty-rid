package com.github.ryan.facility.id.support;


import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>雪花算法ID生成器</b>
 * <p>
 * 基于Twitter雪花算法的分布式ID生成器，生成45位的唯一ID。
 * </p>
 *
 * <h3>ID结构（共45位有效位）：</h3>
 * <pre>
 * | 31位时间戳 | 2位工作节点ID | 2位数据中心ID | 10位序列号 |
 * </pre>
 *
 * <h3>特性：</h3>
 * <ul>
 *     <li>全局唯一：不同数据中心、工作节点生成的ID不会冲突</li>
 *     <li>趋势递增：基于时间戳生成，保证时间上的递增性</li>
 *     <li>高性能：每毫秒可生成1024个ID</li>
 *     <li>时钟回拨检测：防止时钟回拨导致的ID重复</li>
 *     <li>参数校验：防止无效配置导致的ID冲突</li>
 * </ul>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * # application.yml
 * snowflake:
 *   datacenter-id: 1  # 数据中心ID (0-3)
 *   worker-id: 0      # 工作节点ID (0-3)
 * }</pre>
 *
 * @author yvvb
 * @since 2025/4/20
 */
@Setter
@Component
public class SnowIdGenerator {

    // ======================== 常量配置 ========================

    /**
     * 起始时间戳：2025-01-01 00:00:00 UTC+8
     */
    private static final long START_TIMESTAMP = 1735660800000L;

    /**
     * 时间戳占用的位数（约68年）
     */
    private static final int TIMESTAMP_BITS = 31;

    /**
     * 工作节点ID占用的位数
     */
    private static final int WORKER_ID_BITS = 2;

    /**
     * 数据中心ID占用的位数
     */
    private static final int DATA_CENTER_ID_BITS = 2;

    /**
     * 序列号占用的位数
     */
    private static final int SEQUENCE_BITS = 10;

    // ======================== 最大值计算（用于按位与运算） ========================

    /**
     * 时间戳最大值（相对于起始时间）
     */
    private static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BITS) - 1;

    /**
     * 工作节点ID最大值：3
     */
    public static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;

    /**
     * 数据中心ID最大值：3
     */
    public static final long MAX_DATA_CENTER_ID = (1L << DATA_CENTER_ID_BITS) - 1;

    /**
     * 序列号最大值：1023
     */
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // ======================== 位置偏移量 ========================

    /**
     * 时间戳左移位数
     */
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + DATA_CENTER_ID_BITS + WORKER_ID_BITS;

    /**
     * 工作节点ID左移位数
     */
    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS + DATA_CENTER_ID_BITS;

    /**
     * 数据中心ID左移位数
     */
    private static final int DATA_CENTER_ID_SHIFT = SEQUENCE_BITS;

    // ======================== 实例属性 ========================

    /**
     * 数据中心ID，可通过配置文件注入
     */
    @Value("${snowflake.datacenter-id:0}")
    private long dataCenterId;

    /**
     * 工作节点ID，可通过配置文件注入
     */
    @Value("${snowflake.worker-id:0}")
    private long workerId;

    /**
     * 毫秒内序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 是否已初始化（参数已校验）
     */
    private volatile boolean initialized = false;

    /**
     * 默认无参构造函数，供Spring使用
     */
    public SnowIdGenerator() {
        // 默认值由 @Value 注入
    }

    /**
     * <b>构造函数</b>
     *
     * @param dataCenterId 数据中心ID (0-3)
     * @param workerId     工作节点ID (0-3)
     *
     * @throws IllegalArgumentException 参数超出有效范围时抛出
     */
    public SnowIdGenerator(long dataCenterId, long workerId) {
        validateAndSet(dataCenterId, workerId);
    }

    /**
     * Spring Bean 初始化后校验参数
     */
    @PostConstruct
    public void init() {
        if (!initialized) {
            validateAndSet(this.dataCenterId, this.workerId);
        }
    }

    /**
     * 校验并设置数据中心ID和工作节点ID
     */
    private void validateAndSet(long dataCenterId, long workerId) {
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException(
                    String.format("dataCenterId 必须在 0-%d 之间，当前值: %d", MAX_DATA_CENTER_ID, dataCenterId));
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("workerId 必须在 0-%d 之间，当前值: %d", MAX_WORKER_ID, workerId));
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
        this.initialized = true;
    }

    /**
     * <b>生成下一个唯一ID</b>
     * <p>
     * 线程安全的ID生成方法，包含时钟回拨检测和时间戳溢出检测。
     * </p>
     *
     * @return 唯一的雪花ID
     *
     * @throws IllegalStateException 当发生时钟回拨或时间戳溢出时抛出
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();

        // 时钟回拨检测
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= 5) {
                // 小范围回拨，等待追上
                try {
                    Thread.sleep(offset << 1);
                    currentTimestamp = System.currentTimeMillis();
                    if (currentTimestamp < lastTimestamp) {
                        throw new IllegalStateException(
                                String.format("时钟回拨，拒绝生成ID。回拨时间: %d ms", lastTimestamp - currentTimestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("等待时钟同步时被中断", e);
                }
            } else {
                throw new IllegalStateException(
                        String.format("时钟回拨，拒绝生成ID。回拨时间: %d ms", offset));
            }
        }

        // 时间戳溢出检测
        long deltaTimestamp = currentTimestamp - START_TIMESTAMP;
        if (deltaTimestamp > MAX_TIMESTAMP) {
            throw new IllegalStateException(
                    String.format("时间戳溢出，ID生成器已达到最大使用年限。当前时间戳差值: %d，最大值: %d", deltaTimestamp, MAX_TIMESTAMP));
        }

        if (lastTimestamp == currentTimestamp) {
            // 同一毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 当前毫秒序列号耗尽，等待下一毫秒
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // 组装ID
        return (deltaTimestamp << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | sequence;
    }

    /**
     * <b>等待下一毫秒</b>
     * <p>
     * 当当前毫秒序列号耗尽时，自旋等待直到下一毫秒。
     * </p>
     *
     * @param lastTimestamp 上次生成ID的时间戳
     *
     * @return 下一毫秒的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            // 短暂让出CPU，避免过度自旋
            Thread.yield();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    // ======================== ID 解析方法 ========================

    /**
     * <b>从ID中解析时间戳</b>
     *
     * @param id 雪花ID
     *
     * @return 生成该ID时的时间戳（毫秒）
     */
    public static long parseTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
    }

    /**
     * <b>从ID中解析工作节点ID</b>
     *
     * @param id 雪花ID
     *
     * @return 工作节点ID
     */
    public static long parseWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * <b>从ID中解析数据中心ID</b>
     *
     * @param id 雪花ID
     *
     * @return 数据中心ID
     */
    public static long parseDataCenterId(long id) {
        return (id >> DATA_CENTER_ID_SHIFT) & MAX_DATA_CENTER_ID;
    }

    /**
     * <b>从ID中解析序列号</b>
     *
     * @param id 雪花ID
     *
     * @return 序列号
     */
    public static long parseSequence(long id) {
        return id & MAX_SEQUENCE;
    }

    /**
     * <b>获取ID的详细信息（用于调试）</b>
     *
     * @param id 雪花ID
     *
     * @return 包含各部分信息的字符串
     */
    public static String parseInfo(long id) {
        return String.format("ID=%d, timestamp=%d, workerId=%d, dataCenterId=%d, sequence=%d",
                id, parseTimestamp(id), parseWorkerId(id), parseDataCenterId(id), parseSequence(id));
    }
}
