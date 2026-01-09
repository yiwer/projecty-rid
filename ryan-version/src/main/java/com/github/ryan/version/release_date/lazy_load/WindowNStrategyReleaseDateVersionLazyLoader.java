package com.github.ryan.version.release_date.lazy_load;

import com.github.ryan.facility.common.CommonUtil;
import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.core.VersionLazyLoader;
import com.github.ryan.version.release_date.ReleaseDateVersionMetaData;
import com.github.ryan.version.release_date.version.AbstractReleaseDateVersion;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>[k-N, k+N]窗口业务数据懒加载器抽象类</b>
 * <p>
 * 实现窗口策略的懒加载器，当加载某个版本时，会同时加载其前后n个版本。
 * 这种策略可以减少数据库查询次数，提高性能。
 * </p>
 *
 * <h3>加载策略：</h3>
 * <ul>
 *     <li>当访问某个版本时，会查找其前后n个版本（默认n=2）</li>
 *     <li>对于未加载的版本，批量从数据源加载</li>
 *     <li>已加载的版本不会重复加载</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class StopWindowLoader extends WindowNStrategyVersionLazyLoader<StopData> {
 *     public StopWindowLoader(LazyLoadableReleaseDateVersionChain<StopData> chain) {
 *         super(chain, 3); // 窗口大小为3
 *     }
 *
 *     @Override
 *     public void batchLoadVersions(Long dataId, Set<DVersionIdentity> lazyVersionIdentities) {
 *         // 批量加载数据并加载到版本链
 *     }
 * }
 * }</pre>
 *
 * @param <D> 聚合数据类型
 *
 * @author yvvb
 * @see VersionLazyLoader
 * @see LazyLoadableReleaseDateVersionChain
 * @since 11/6/2025
 */
public abstract class WindowNStrategyReleaseDateVersionLazyLoader<D extends BusinessData<D>>
        implements VersionLazyLoader<D, LazyLoadableReleaseDateVersion<D>> {

    /**
     * 关联的版本链
     */
    protected final LazyLoadableReleaseDateVersionChain<D> versionChain;
    /**
     * 窗口大小，表示前后n个版本
     */
    private final int n;

    /**
     * 默认构造函数
     * <p>
     * 使用默认窗口大小2
     * </p>
     *
     * @param versionChain 版本链
     */
    protected WindowNStrategyReleaseDateVersionLazyLoader(LazyLoadableReleaseDateVersionChain<D> versionChain) {
        this(versionChain, 2);
    }

    /**
     * 带窗口大小的构造函数
     *
     * @param versionChain 版本链
     * @param n            窗口大小
     */
    protected WindowNStrategyReleaseDateVersionLazyLoader(LazyLoadableReleaseDateVersionChain<D> versionChain, int n) {
        this.n = n;
        this.versionChain = versionChain;
    }

    /**
     * 加载业务数据
     * <p>
     * 当加载某个版本时，会同时加载其前后n个版本中未加载的数据
     * </p>
     *
     * @param dataId          数据ID
     * @param dataType        数据类型
     * @param lazyLoadVersion 懒加载版本
     *
     * @return 加载的业务数据
     *
     * @throws RuntimeException 加载失败时抛出
     */
    @Override
    public D load(Long dataId, Integer dataType, LazyLoadableReleaseDateVersion<D> lazyLoadVersion) {
        final List<LazyLoadableReleaseDateVersion<D>> windowVersions = versionChain.findVersionsInWindowN(lazyLoadVersion, n);
        if (CommonUtil.isNotEmpty(windowVersions)) {
            final Set<ReleaseDateVersionMetaData> releaseDateVersionIdentities = windowVersions.stream()
                    .filter(v -> !v.isLoaded())
                    .map(AbstractReleaseDateVersion::getReleaseDateVersionMetaData)
                    .collect(Collectors.toSet());
            if (CommonUtil.isNotEmpty(releaseDateVersionIdentities)) {
                batchLoadVersions(dataId, releaseDateVersionIdentities);
            }
        }
        return lazyLoadVersion.tryGetBusinessDataSkipLoad().orElseThrow(() ->
                new RuntimeException("Load businessData failed!" + "[dataId:" + dataId + ", dataType:" + dataType + "]")
        );
    }

    /**
     * 批量加载版本数据
     * <p>
     * 由子类实现，从数据源批量加载指定版本的业务数据，
     * 一定要调用{@link LazyLoadableReleaseDateVersionChain#loadAggregatedDataToChain}加载到版本链
     * </p>
     *
     * @param dataId                       数据ID
     * @param releaseDateVersionIdentities 需要加载的版本身份标识集合
     */
    public abstract void batchLoadVersions(Long dataId, Set<ReleaseDateVersionMetaData> releaseDateVersionIdentities);
}
