package com.github.ryan.version.release_date;

import com.github.ryan.version.core.BusinessData;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * <b>日期版本变更点接口</b>
 * <p>
 * 定义在特定日期对业务数据进行变更的规范。
 * 变更点包含变更日期、变更逻辑和变更类型三个要素。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>批量处理多个日期的变更</li>
 *     <li>统一定义变更行为</li>
 *     <li>与版本链的 {@code duelChangePoint} 方法配合使用</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ReleaseDateProcessPoint<StopData> changePoint = new ReleaseDateProcessPoint<>() {
 *     @Override
 *     public LocalDate getReleaseDate() {
 *         return LocalDate.of(2025, 6, 1);
 *     }
 *
 *     @Override
 *     public Consumer<StopData> getChangeConsumer() {
 *         return stop -> stop.setName("新站点名称");
 *     }
 *
 *     @Override
 *     public ReleaseVersionType getReleaseVersionType() {
 *         return ReleaseVersionType.MODIFY;
 *     }
 * };
 *
 * versionChain.duelChangePoint(changePoint);
 * }</pre>
 *
 * @param <D> 业务数据类型
 *
 * @author yvvb
 * @see com.github.ryan.version.release_date.chain.ReleaseDateVersionChain#duelChangePoint
 * @since 11/5/2025
 */
public interface ReleaseDateProcessPoint<D extends BusinessData<D>> {
    /**
     * 获取变更日期
     * <p>
     * 该日期表示变更生效的日期
     * </p>
     *
     * @return 变更日期
     */
    LocalDate getReleaseDate();

    /**
     * 获取变更消费者
     * <p>
     * 定义如何修改业务数据的逻辑
     * </p>
     *
     * @return 变更消费者函数
     */
    Consumer<D> getChangeConsumer();

    /**
     * 获取版本类型
     * <p>
     * 表示该变更点是新增、修改还是删除
     * </p>
     *
     * @return 版本类型
     */
    ReleaseVersionType getReleaseVersionType();
}
