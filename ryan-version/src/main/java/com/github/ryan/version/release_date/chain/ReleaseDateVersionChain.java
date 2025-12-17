package com.github.ryan.version.release_date.chain;


import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.result.Result;
import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.core.BusinessDataModifyInterface;
import com.github.ryan.version.release_date.ReleaseDateProcessPoint;
import com.github.ryan.version.release_date.version.AbstractReleaseDateVersion;
import com.github.ryan.version.release_date.version.AloneReleaseDateVersion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <b>日期聚合版本链规范接口</b>
 * <p>
 * 定义数据版本链的核心操作，包括版本查询、修改、删除、移动等。
 * 版本链以日期为基础维度管理数据的多个版本。
 * </p>
 *
 * <h3>核心概念：</h3>
 * <ul>
 *     <li>版本日期(versionDate)：版本生效的起始日期</li>
 *     <li>过期日期(expirationDate)：版本失效的日期</li>
 *     <li>创始日期(genesisDate)：数据创建的日期（第一个版本的日期）</li>
 * </ul>
 *
 * @param <D> 聚合数据类型
 * @param <V> 版本类型
 *
 * @author yvvb
 * @see AbstractReleaseDateVersion
 * @see BusinessData
 * @since 11/5/2025
 */
public interface ReleaseDateVersionChain<D extends BusinessData<D>, V extends AbstractReleaseDateVersion<D, V>> {

    /**
     * 获取数据ID
     *
     * @return 数据的唯一标识
     */
    Long getDataId();

    /**
     * 查找窗口N范围内的版本
     * <p>
     * 以指定日期为中心，获取前后n个版本
     * </p>
     *
     * @param versionDate 版本日期
     * @param n           窗口大小
     *
     * @return 窗口范围内的版本列表
     */
    List<V> findVersionsInWindowN(LocalDate versionDate, int n);

    /**
     * 查找窗口N范围内的版本
     * <p>
     * 以指定版本为中心，获取前后n个版本
     * </p>
     *
     * @param version 中心版本
     * @param n       窗口大小
     *
     * @return 窗口范围内的版本列表
     */
    List<V> findVersionsInWindowN(AbstractReleaseDateVersion<D, V> version, int n);

    /**
     * 获取创始日期
     * <p>
     * 返回版本链的第一个版本日期，即数据创建日期
     * </p>
     *
     * @return 创始日期，版本链为空时返回null
     */
    LocalDate getGenesisDate();

    /**
     * 获取删除日期
     * <p>
     * 如果数据已被删除，返回删除版本的日期
     * </p>
     *
     * @return 删除日期的Optional，未删除时返回空
     */
    Optional<LocalDate> getDeletedDate();

    /**
     * 查找生效版本
     * <p>
     * 查找在指定日期生效的版本（版本日期 <= 查询日期）
     * </p>
     *
     * @param queryDate 查询日期
     *
     * @return 生效版本的Optional
     */
    Optional<V> findEffectiveVersion(LocalDate queryDate);

    /**
     * 根据版本日期精确查找版本
     *
     * @param versionDate 版本日期
     *
     * @return 版本的Optional
     */
    Optional<V> findVersionByVersionDateEq(LocalDate versionDate);

    /**
     * 根据过期日期精确查找版本
     *
     * @param expirationDate 过期日期
     *
     * @return 版本的Optional
     */
    Optional<V> findVersionByExpirationDateEq(LocalDate expirationDate);

    /**
     * 查找前一个版本
     *
     * @param dataVersion 当前版本
     *
     * @return 前一个版本的Optional
     */
    Optional<V> findPrecedingVersion(V dataVersion);

    /**
     * 查找后一个版本
     *
     * @param dataVersion 当前版本
     *
     * @return 后一个版本的Optional
     */
    Optional<V> findFollowingVersion(V dataVersion);

    /**
     * 获取所有版本
     *
     * @return 所有版本的集合
     */
    Collection<V> getAllVersions();

    /**
     * 获取所有已变更的版本
     * <p>
     * 用于持久化时识别哪些版本需要更新
     * </p>
     *
     * @return 已变更版本的集合
     */
    Collection<V> getChangedVersions();

    /**
     * 获取所有已移除的版本
     * <p>
     * 用于持久化时识别哪些版本需要删除
     * </p>
     *
     * @return 已移除版本的列表
     */
    List<AloneReleaseDateVersion<D>> getRemovedVersions();

    /**
     * 获取当前版本之后的所有版本
     *
     * @param current 当前版本
     *
     * @return 后续版本的集合
     */
    Collection<V> getBehindVersions(V current);

    /**
     * 获取当前版本之前的所有版本
     *
     * @param current 当前版本
     *
     * @return 前置版本的集合
     */
    Collection<V> getPrecedingVersions(V current);

    /**
     * 根据版本日期范围查询版本
     *
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     *
     * @return 范围内的版本集合
     */
    Collection<V> getVersionsByVersionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 根据过期日期范围查询版本
     *
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     *
     * @return 范围内的版本集合
     */
    Collection<V> getVersionsByExpirationDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 查询与日期范围有交集的版本
     * <p>
     * 返回版本日期<=endDate 且 过期日期>=startDate 的版本
     * </p>
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     *
     * @return 与范围有交集的版本集合
     */
    Collection<V> getVersionsByRangeCross(LocalDate startDate, LocalDate endDate);

    /**
     * 检查指定日期范围内是否存在版本
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     *
     * @return true-存在版本，false-不存在
     */
    boolean haveVersionInRangeDate(LocalDate startDate, LocalDate endDate);

    /**
     * 修改指定日期的版本数据
     * <p>
     * 如果修改日期与现有版本日期相同，则合并修改；
     * 否则分裂出新版本
     * </p>
     *
     * @param modifyDate  修改日期
     * @param modifyModel 修改模型
     *
     * @return 操作结果
     */
    Result<Void, WrappedError> modify(LocalDate modifyDate, BusinessDataModifyInterface modifyModel);

    /**
     * 删除指定日期的数据版本
     * <p>
     * 标记数据在指定日期后被删除
     * </p>
     *
     * @param deleteDate 删除日期
     *
     * @return 操作结果
     */
    Result<Void, WrappedError> delete(LocalDate deleteDate);

    /**
     * 移动版本到目标日期
     * <p>
     * 将版本从原日期移动到目标日期，不能跨越其他版本
     * </p>
     *
     * @param originVersion 原版本
     * @param targetDate    目标日期
     *
     * @return 操作结果
     */
    Result<Void, WrappedError> move(V originVersion, LocalDate targetDate);

    /**
     * 移除指定版本
     * <p>
     * 从版本链中移除指定版本，并调整前一个版本的过期日期
     * </p>
     *
     * @param removedVersion 要移除的版本
     *
     * @return 操作结果
     */
    Result<Void, WrappedError> remove(V removedVersion);

    /**
     * 处理版本变更点
     * <p>
     * 根据变更点定义的日期和变更逻辑处理版本变更
     * </p>
     *
     * @param changePoint 变更点
     *
     * @return 操作结果
     */
    Result<Void, WrappedError> duelChangePoint(ReleaseDateProcessPoint<D> releasePoint);
}
