package com.github.ryan.version.core;

import java.util.Optional;

/**
 * <b>懒加载版本接口</b>
 * <p>
 * 定义版本的懒加载能力。实现此接口的版本可以延迟加载业务数据，
 * 以减少内存占用和初始化时间。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>版本链包含大量版本，但只需访问部分版本</li>
 *     <li>业务数据较大，全量加载开销过高</li>
 *     <li>查询场景不需要访问业务数据详情</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 检查是否已加载
 * if (!version.isLoaded()) {
 *     // 从数据库加载数据
 *     StopData data = stopRepository.findById(version.getDataId());
 *     version.load(data);
 * }
 *
 * // 尝试获取已加载的数据（不触发加载）
 * Optional<StopData> data = version.tryGetBusinessDataSkipLoad();
 * data.ifPresent(d -> System.out.println(d.getStopName()));
 * }</pre>
 *
 * @param <D> 业务数据类型
 *
 * @author yvvb
 * @see VersionLazyLoader
 * @since 11/5/2025
 */
public interface LazyLoadableVersion<D extends BusinessData<D>> {

    /**
     * 检查业务数据是否已加载
     *
     * @return true-已加载，false-未加载
     */
    boolean isLoaded();

    /**
     * 加载业务数据
     * <p>
     * 将业务数据加载到版本中，并标记为已加载状态
     * </p>
     *
     * @param businessData 要加载的业务数据
     */
    void load(D businessData);

    /**
     * 尝试获取业务数据（跳过加载）
     * <p>
     * 返回当前已加载的业务数据，不触发加载操作。
     * 用于仅需访问已加载数据的场景。
     * </p>
     *
     * @return 业务数据的 Optional，未加载时返回空
     */
    Optional<D> tryGetBusinessDataSkipLoad();
}
