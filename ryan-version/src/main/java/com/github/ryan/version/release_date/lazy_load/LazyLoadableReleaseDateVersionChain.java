package com.github.ryan.version.release_date.lazy_load;

import com.github.ryan.facility.common.CommonUtil;
import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.core.VersionLazyLoader;
import com.github.ryan.version.release_date.ReleaseDateVersionData;
import com.github.ryan.version.release_date.chain.AbstractReleaseDateVersionChain;

import java.util.List;
import java.util.Map;

/**
 * <b>懒加载日期版本链抽象类</b>
 * <p>
 * 支持懒加载模式的日期版本链实现。
 * 业务数据只在需要时才会被加载，以减少内存占用和初始化时间。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>版本链包含大量版本但只需访问部分版本</li>
 *     <li>业务数据较大，全量加载开销过高</li>
 * </ul>
 *
 * @param <D> 聚合数据类型
 *
 * @author yvvb
 * @see LazyLoadableReleaseDateVersion
 * @see VersionLazyLoader
 * @since 11/5/2025
 */
public abstract class LazyLoadableReleaseDateVersionChain<D extends BusinessData<D>>
        extends AbstractReleaseDateVersionChain<D, LazyLoadableReleaseDateVersion<D>> {

    /**
     * 从单个版本构造版本链
     *
     * @param identity       版本身份标识
     * @param dataType       数据类型
     * @param aggregatedData 业务数据
     */
    protected LazyLoadableReleaseDateVersionChain(ReleaseDateVersionData identity, D businessData) {
        super(identity.getDataId(), identity.getDataType(), List.of(new LazyLoadableReleaseDateVersion<D>(identity, businessData)));
    }

    /**
     * 从多个懒加载版本构造版本链
     *
     * @param dataId               数据ID
     * @param dataType             数据类型
     * @param lazyLoadableVersions 懒加载版本列表
     */
    protected LazyLoadableReleaseDateVersionChain(Long dataId, Integer dataType, List<LazyLoadableReleaseDateVersion<D>> lazyLoadableVersions) {
        super(dataId, dataType, lazyLoadableVersions);
    }

    /**
     * 将业务数据加载到版本链
     * <p>
     * 根据版本身份标识将业务数据加载到对应的版本中
     * </p>
     *
     * @param lazyLoadedDataMap 版本身份标识到业务数据的映射
     */
    public void loadAggregatedDataToChain(Map<ReleaseDateVersionData, D> lazyLoadedDataMap) {
        if (CommonUtil.isNotEmpty(lazyLoadedDataMap)) {
            lazyLoadedDataMap.forEach((lazyIdentity, businessData) -> {
                this.findVersionByVersionDateEq(lazyIdentity.getVersionDate()).ifPresent(version ->
                        version.load(businessData)
                );
            });
        }
    }
}
