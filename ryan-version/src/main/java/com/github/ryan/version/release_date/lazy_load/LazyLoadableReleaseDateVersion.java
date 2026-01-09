package com.github.ryan.version.release_date.lazy_load;

import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.core.LazyLoadableVersion;
import com.github.ryan.version.core.VersionLazyLoader;
import com.github.ryan.version.release_date.ReleaseDateVersionMetaData;
import com.github.ryan.version.release_date.ReleaseVersionType;
import com.github.ryan.version.release_date.version.AbstractReleaseDateVersion;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Optional;

/**
 * <b>懒加载日期版本</b>
 * <p>
 * 支持懒加载的日期版本实现。业务数据可以在首次访问时才从数据源加载，
 * 以减少内存占用和初始化时间。
 * </p>
 *
 * <h3>加载机制：</h3>
 * <ul>
 *     <li>调用 {@link #getBusinessData()} 时，如果数据未加载，会通过 versionLoader 加载</li>
 *     <li>调用 {@link #tryGetBusinessDataSkipLoad()} 不会触发加载</li>
 *     <li>调用 {@link #load(BusinessData)} 可以手动加载数据</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建懒加载版本（未加载数据）
 * LazyLoadableReleaseDateVersion<StopData> version =
 *     new LazyLoadableReleaseDateVersion<>(versionData);
 * version.setVersionLoader(myLoader);  // 设置加载器
 *
 * // 访问时自动加载
 * StopData data = version.getBusinessData();  // 触发加载
 *
 * // 也可以手动加载
 * StopData preloadedData = repository.findById(dataId);
 * version.load(preloadedData);
 * }</pre>
 *
 * @param <D> 业务数据类型
 *
 * @author yvvb
 * @see LazyLoadableVersion
 * @see VersionLazyLoader
 * @see WindowNStrategyReleaseDateVersionLazyLoader
 * @since 11/5/2025
 */
public class LazyLoadableReleaseDateVersion<D extends BusinessData<D>>
        extends AbstractReleaseDateVersion<D, LazyLoadableReleaseDateVersion<D>>
        implements LazyLoadableVersion<D> {

    /**
     * 业务数据（懒加载，初始为 null）
     */
    private D businessData = null;

    /**
     * 标记业务数据是否已加载
     */
    @Getter
    private boolean loaded = false;

    /**
     * 版本懒加载器，用于加载业务数据
     */
    @Setter
    private VersionLazyLoader<D, LazyLoadableReleaseDateVersion<D>> versionLoader = null;

    /**
     * 构造函数（未加载数据）
     *
     * @param releaseDateVersionMetaData 版本元数据
     */
    protected LazyLoadableReleaseDateVersion(@Nonnull ReleaseDateVersionMetaData releaseDateVersionMetaData) {
        super(releaseDateVersionMetaData);
    }

    /**
     * 构造函数（已加载数据）
     *
     * @param releaseDateVersionMetaData 版本元数据
     * @param businessData               业务数据
     */
    protected LazyLoadableReleaseDateVersion(@Nonnull ReleaseDateVersionMetaData releaseDateVersionMetaData, @Nonnull D businessData) {
        super(releaseDateVersionMetaData);
        this.businessData = businessData;
        this.loaded = true;
    }

    /**
     * 获取业务数据
     * <p>
     * 如果数据未加载，会通过 versionLoader 自动加载。
     * </p>
     *
     * @return 业务数据
     *
     * @throws IllegalStateException 如果 versionLoader 未设置
     * @throws VersionLoadException  如果加载失败
     */
    @Override
    public D getBusinessData() {
        if (this.businessData == null) {
            if (this.versionLoader == null) {
                throw new IllegalStateException("VersionLoader not set for lazy loading");
            }
            final Long dataId = super.getDataId();
            final Integer dataType = super.getDataType();
            D data = this.versionLoader.load(dataId, dataType, this);
            if (data == null) {
                throw new VersionLoadException(dataId, dataType);
            }
        }
        return this.businessData;
    }

    /**
     * 创建分裂版本
     * <p>
     * 从当前版本分裂出一个新版本，新版本从指定日期开始生效。
     * 新版本会复制当前版本的业务数据（深拷贝）。
     * </p>
     *
     * @param modifyDate 新版本的生效日期
     *
     * @return 新创建的版本
     */
    @Override
    public LazyLoadableReleaseDateVersion<D> splitNextVersion(LocalDate modifyDate) {
        final ReleaseDateVersionMetaData nextVersionData = ReleaseDateVersionMetaData.builder()
                .dataId(super.getDataId())
                .dataType(super.getDataType())
                .releaseVersionType(ReleaseVersionType.MODIFY)
                .versionDate(modifyDate)
                .expirationDate(super.getExpirationDate())
                .dataStatus(super.getDataStatus())
                .build();
        return new LazyLoadableReleaseDateVersion<>(nextVersionData, this.businessData.copy());
    }

    /**
     * 加载业务数据
     * <p>
     * 手动加载业务数据到当前版本，并标记为已加载状态。
     * 如果传入 null，则不会进行任何操作。
     * </p>
     *
     * @param businessData 要加载的业务数据
     */
    @Override
    public void load(D businessData) {
        if (businessData != null) {
            this.businessData = businessData;
            this.loaded = true;
        }
    }

    /**
     * 尝试获取业务数据（跳过加载）
     * <p>
     * 返回当前已加载的业务数据，不触发加载操作。
     * 用于仅需访问已加载数据的场景。
     * </p>
     *
     * @return 业务数据的 Optional，未加载时返回空
     */
    @Override
    public Optional<D> tryGetBusinessDataSkipLoad() {
        return Optional.ofNullable(this.businessData);
    }
}
