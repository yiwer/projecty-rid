package com.github.ryan.version.release_date.version;

import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.release_date.ReleaseDateVersionData;
import com.github.ryan.version.release_date.lazy_load.LazyLoadableReleaseDateVersion;
import lombok.Getter;

import java.time.LocalDate;

/**
 * <b>独立日期版本</b>
 * <p>
 * 直接持有业务数据的版本实现。不支持懒加载，业务数据在构造时即确定。
 * 主要用于存储已移除的版本快照或不需要懒加载的场景。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>存储已从版本链中移除的版本，用于后续删除操作</li>
 *     <li>版本数量较少，不需要懒加载的场景</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <p>
 * 此类不支持 {@link #splitNextVersion(LocalDate)} 操作，
 * 调用时会抛出 {@link UnsupportedOperationException}。
 * 如需版本分裂功能，请使用 {@link LazyLoadableReleaseDateVersion}。
 * </p>
 *
 * @param <D> 业务数据类型
 *
 * @author yvvb
 * @see AbstractReleaseDateVersion
 * @see LazyLoadableReleaseDateVersion
 * @since 11/5/2025
 */
@Getter
public class AloneReleaseDateVersion<D extends BusinessData<D>>
        extends AbstractReleaseDateVersion<D, AloneReleaseDateVersion<D>> {

    /**
     * 业务数据（直接持有）
     */
    protected final D businessData;

    /**
     * 构造函数
     *
     * @param versionData  版本元数据
     * @param businessData 业务数据
     */
    public AloneReleaseDateVersion(ReleaseDateVersionData versionData, D businessData) {
        super(versionData);
        this.businessData = businessData;
    }

    /**
     * 创建分裂版本
     * <p>
     * 此实现不支持分裂操作，调用时将抛出异常
     * </p>
     *
     * @param modifyDate 修改日期
     *
     * @return 不返回，始终抛出异常
     *
     * @throws UnsupportedOperationException 始终抛出
     */
    @Override
    public AloneReleaseDateVersion<D> splitNextVersion(LocalDate modifyDate) {
        throw new UnsupportedOperationException("Unsupported operation for DefaultReleaseDateVersion");
    }
}
