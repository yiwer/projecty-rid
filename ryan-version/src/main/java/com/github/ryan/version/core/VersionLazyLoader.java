package com.github.ryan.version.core;

/**
 * <b>版本数据懒加载器接口</b>
 * <p>
 * 定义如何从数据源（如数据库、缓存等）加载业务数据。
 * 实现类需要根据具体的数据存储方式实现加载逻辑。
 * </p>
 *
 * @param <D> 聚合数据类型
 *
 * @author yvvb
 * @see LazyLoadableVersion
 * @since 11/5/2025
 */
public interface VersionLazyLoader<D extends BusinessData<D>, V extends LazyLoadableVersion<D>> {

    /**
     * 加载业务数据
     * <p>
     * 根据数据ID、数据类型和版本信息加载对应的业务数据
     * </p>
     *
     * @param dataId          数据ID
     * @param dataType        数据类型
     * @param lazyLoadVersion 懒加载版本（包含版本日期等信息）
     *
     * @return 加载的业务数据，加载失败时可返回null
     */
    D load(Long dataId, Integer dataType, V lazyLoadVersion);

}
