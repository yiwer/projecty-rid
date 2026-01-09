package com.github.ryan.version.release_date.version;

import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.core.BusinessDataModifyInterface;
import com.github.ryan.version.release_date.ReleaseDateVersionMetaData;
import com.github.ryan.version.release_date.ReleaseVersionType;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * <b>日期版本抽象基类</b>
 * <p>
 * 定义基于日期的版本核心属性和行为。每个版本都有明确的生效日期范围，
 * 由 versionDate（生效日期）和 expirationDate（过期日期）确定。
 * </p>
 *
 * <h3>核心概念：</h3>
 * <ul>
 *     <li><b>versionDate</b>：版本生效的起始日期（包含）</li>
 *     <li><b>expirationDate</b>：版本失效的日期（包含）</li>
 *     <li><b>changed</b>：标记版本是否已修改，用于持久化时识别</li>
 *     <li><b>removed</b>：标记版本是否已从链中移除</li>
 * </ul>
 *
 * <h3>版本分裂：</h3>
 * <p>
 * 当在版本有效期内的某一天进行修改时，会调用 {@link #splitNextVersion(LocalDate)}
 * 将当前版本分裂为两个版本：修改前和修改后。
 * </p>
 *
 * <pre>
 * 分裂前：
 * |-------- 版本A --------|  [2025-01-01 ~ 2025-12-31]
 *
 * 分裂后（在 2025-06-01 修改）：
 * |-- 版本A --|-- 版本B --|  [2025-01-01 ~ 2025-05-31] + [2025-06-01 ~ 2025-12-31]
 * </pre>
 *
 * @param <D> 业务数据类型
 * @param <V> 版本类型（自引用泛型，用于返回正确的子类型）
 *
 * @author yvvb
 * @see ReleaseDateVersionMetaData
 * @see ReleaseVersionType
 * @since 11/5/2025
 */

@Getter
@Setter
public abstract class AbstractReleaseDateVersion<D extends BusinessData<D>, V extends AbstractReleaseDateVersion<D, V>> {

    /**
     * 版本元数据，包含数据ID、类型、日期等信息
     */
    protected final ReleaseDateVersionMetaData releaseDateVersionMetaData;

    /**
     * 标记版本是否已从链中移除
     */
    protected boolean removed;

    /**
     * 标记版本是否已修改，用于持久化时识别需要更新的版本
     */
    protected boolean changed;

    /**
     * 构造函数
     *
     * @param releaseDateVersionMetaData 版本元数据，不允许为 null
     *
     * @throws NullPointerException 如果 releaseDateVersionData 为 null
     */
    protected AbstractReleaseDateVersion(@Nonnull ReleaseDateVersionMetaData releaseDateVersionMetaData) {
        Objects.requireNonNull(releaseDateVersionMetaData);
        this.releaseDateVersionMetaData = releaseDateVersionMetaData;
    }

    /**
     * 获取业务数据
     * <p>
     * 由子类实现，可以是直接持有或懒加载
     * </p>
     *
     * @return 业务数据实例
     */
    public abstract D getBusinessData();


    /**
     * 修改业务数据
     * <p>
     * 使用修改模型更新业务数据
     * </p>
     *
     * @param modifyModel 修改模型
     */
    public void modifyBusinessData(BusinessDataModifyInterface modifyModel) {
        getBusinessData().modifyData(modifyModel);
    }


    /**
     * 创建分裂版本
     * <p>
     * 从当前版本分裂出一个新版本，新版本从指定日期开始生效
     * </p>
     *
     * @param modifyDate 新版本的生效日期
     *
     * @return 新创建的版本
     */
    public abstract V splitNextVersion(LocalDate modifyDate);


    // ==================== 代理方法 ====================
    // 以下方法均代理到 releaseDateVersionData，提供更便捷的访问方式

    /**
     * 获取数据ID
     */

    public Long getDataId() {
        return releaseDateVersionMetaData.getDataId();
    }

    /**
     * 获取数据类型
     */
    public Integer getDataType() {
        return releaseDateVersionMetaData.getDataType();
    }

    /**
     * 获取版本类型（创建/修改/删除）
     */
    public ReleaseVersionType getReleaseVersionType() {
        return releaseDateVersionMetaData.getReleaseVersionType();
    }

    /**
     * 设置版本类型
     */
    public void setReleaseVersionType(ReleaseVersionType releaseVersionType) {
        releaseDateVersionMetaData.setReleaseVersionType(releaseVersionType);
    }

    /**
     * 获取版本生效日期
     */
    public LocalDate getVersionDate() {
        return releaseDateVersionMetaData.getVersionDate();
    }

    /**
     * 设置版本生效日期
     */
    public void setVersionDate(LocalDate versionDate) {
        releaseDateVersionMetaData.setVersionDate(versionDate);
    }

    /**
     * 获取版本过期日期
     */
    public LocalDate getExpirationDate() {
        return releaseDateVersionMetaData.getExpirationDate();
    }

    /**
     * 设置版本过期日期
     */
    public void setExpirationDate(LocalDate expirationDate) {
        releaseDateVersionMetaData.setExpirationDate(expirationDate);
    }

    /**
     * 获取数据状态
     */
    public Short getDataStatus() {
        return releaseDateVersionMetaData.getDataStatus();
    }

    /**
     * 设置数据状态
     */
    public void setDataStatus(Short dataStatus) {
        releaseDateVersionMetaData.setDataStatus(dataStatus);
    }

    /**
     * 检查数据是否已删除
     */
    public boolean isDeleted() {
        return releaseDateVersionMetaData.isDeleted();
    }

    /**
     * 设置删除状态
     */
    public void setDeleted(boolean deleted) {
        releaseDateVersionMetaData.setDeleted(deleted);
    }

}
