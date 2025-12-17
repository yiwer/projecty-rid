package com.github.ryan.version.release_date.lazy_load;

/**
 * <b>版本加载异常</b>
 * <p>
 * 当懒加载机制无法加载业务数据时抛出此异常。
 * 可能的原因包括：
 * </p>
 * <ul>
 *     <li>数据库中不存在对应的数据</li>
 *     <li>懒加载器未正确配置</li>
 *     <li>数据加载后未正确赋值到版本对象</li>
 * </ul>
 *
 * <h3>异常信息：</h3>
 * <p>包含 dataId 和 dataType，方便定位加载失败的具体数据</p>
 *
 * @author yvvb
 * @see LazyLoadableReleaseDateVersion
 * @since 11/5/2025
 */
public class VersionLoadException extends RuntimeException {

    /**
     * 构造版本加载异常
     *
     * @param dataId   加载失败的数据 ID
     * @param dataType 加载失败的数据类型
     */
    public VersionLoadException(Long dataId, Integer dataType) {
        super("Load businessData failed! [dataId:" + dataId + ", dataType:" + dataType + "]");
    }
}