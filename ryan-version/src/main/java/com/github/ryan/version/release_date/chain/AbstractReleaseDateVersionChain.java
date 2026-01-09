package com.github.ryan.version.release_date.chain;


import cn.hutool.core.collection.CollUtil;
import com.github.ryan.facility.date.DateUtil;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.log.LogUtil;
import com.github.ryan.facility.result.Result;
import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.core.BusinessDataModifyInterface;
import com.github.ryan.version.release_date.ReleaseDateProcessPoint;
import com.github.ryan.version.release_date.ReleaseDateVersionErrorType;
import com.github.ryan.version.release_date.ReleaseVersionType;
import com.github.ryan.version.release_date.version.AbstractReleaseDateVersion;
import com.github.ryan.version.release_date.version.AloneReleaseDateVersion;
import lombok.Getter;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/**
 * <b>日期版本链抽象基类</b>
 * <p>
 * 提供日期版本链的核心实现，使用 TreeMap 存储版本以支持高效的日期范围查询。
 * 支持版本的新增、修改、删除、移动等操作，并自动处理版本分裂逻辑。
 * </p>
 *
 * <h3>数据结构：</h3>
 * <pre>
 * versionDateTreeMap:    [2025-01-01] -> V1, [2025-06-01] -> V2, [2025-12-01] -> V3
 * expirationDateTreeMap: [2025-05-31] -> V1, [2025-11-30] -> V2, [9999-12-31] -> V3
 * </pre>
 *
 * <h3>核心操作：</h3>
 * <ul>
 *     <li>版本分裂：在版本有效期内修改时，将版本一分为二</li>
 *     <li>版本合并：在版本日期当天修改时，直接合并变更</li>
 *     <li>版本移动：将版本移动到新日期（不能跨越其他版本）</li>
 *     <li>版本删除：标记数据删除并移除后续版本</li>
 * </ul>
 *
 * <h3>版本分裂示意图：</h3>
 * <pre>
 * 分裂前：
 * |-------- 版本A --------|  [2025-01-01 ~ 2025-12-31]
 *
 * 分裂后（在 2025-06-01 修改）：
 * |-- 版本A --|-- 版本B --|  [2025-01-01 ~ 2025-05-31] + [2025-06-01 ~ 2025-12-31]
 * </pre>
 *
 * @param <D> 业务数据类型
 * @param <V> 版本类型
 *
 * @author yvvb
 * @see ReleaseDateVersionChain
 * @see AbstractReleaseDateVersion
 * @since 11/5/2025
 */
public abstract class AbstractReleaseDateVersionChain<D extends BusinessData<D>, V extends AbstractReleaseDateVersion<D, V>>
        implements ReleaseDateVersionChain<D, V> {
    /**
     * 数据唯一标识
     */
    @Getter
    private final Long dataId;
    /**
     * 数据类型
     */
    @Getter
    private final Integer dataType;
    /**
     * 按版本日期索引的版本映射
     */
    private final TreeMap<LocalDate, V> versionDateTreeMap = new TreeMap<>();
    /**
     * 按过期日期索引的版本映射
     */
    private final TreeMap<LocalDate, V> expirationDateTreeMap = new TreeMap<>();
    /**
     * 已移除的版本列表，用于持久化时删除
     */
    List<AloneReleaseDateVersion<D>> removeVersions = new ArrayList<>();

    /**
     * 构造日期版本链
     *
     * @param dataId              数据 ID
     * @param dataType            数据类型
     * @param releaseDateVersions 初始版本列表
     */
    protected AbstractReleaseDateVersionChain(Long dataId, Integer dataType, List<V> releaseDateVersions) {
        this.dataId = dataId;
        this.dataType = dataType;
        if (CollUtil.isNotEmpty(releaseDateVersions)) {
            releaseDateVersions.forEach(version -> {
                this.versionDateTreeMap.putIfAbsent(version.getVersionDate(), version);
                this.expirationDateTreeMap.putIfAbsent(version.getExpirationDate(), version);
            });
        }
    }

    /**
     * 向版本链添加版本
     * <p>同时更新两个索引映射，并标记版本为已变更</p>
     *
     * @param version 要添加的版本
     */
    protected void addVersionToChain(V version) {
        version.setChanged(true);
        this.versionDateTreeMap.put(version.getVersionDate(), version);
        this.expirationDateTreeMap.put(version.getExpirationDate(), version);
    }

    /**
     * 从版本链移除版本
     * <p>将版本移至已移除列表，并从两个索引映射中删除</p>
     *
     * @param version 要移除的版本
     */
    protected void removeVersionInChain(V version) {
        version.setRemoved(true);
        this.removeVersions.add(new AloneReleaseDateVersion<>(version.getReleaseDateVersionMetaData(), version.getBusinessData()));
        this.versionDateTreeMap.remove(version.getVersionDate());
        this.expirationDateTreeMap.remove(version.getExpirationDate());
    }

    /**
     * 刷新版本的版本日期索引
     *
     * @param version        版本对象
     * @param oldVersionDate 原版本日期
     */
    protected void refreshVersionVersionDateInChain(V version, LocalDate oldVersionDate) {
        version.setChanged(true);
        this.versionDateTreeMap.remove(oldVersionDate);
        this.versionDateTreeMap.put(version.getVersionDate(), version);
    }

    /**
     * 刷新版本的过期日期索引
     *
     * @param version           版本对象
     * @param oldExpirationDate 原过期日期
     */
    protected void refreshVersionExpirationDateInChain(V version, LocalDate oldExpirationDate) {
        version.setChanged(true);
        this.expirationDateTreeMap.remove(oldExpirationDate);
        this.expirationDateTreeMap.put(version.getExpirationDate(), version);
    }


    /**
     * 变更版本的生效日期
     *
     * @param dataVersion 版本对象
     * @param versionDate 新的生效日期
     */
    public void changeVersionDate(V dataVersion, LocalDate versionDate) {
        final LocalDate oldVersionDate = dataVersion.getVersionDate();
        dataVersion.setVersionDate(versionDate);
        dataVersion.setChanged(true);
        refreshVersionVersionDateInChain(dataVersion, oldVersionDate);
    }

    /**
     * 变更版本的过期日期
     *
     * @param dataVersion    版本对象
     * @param expirationDate 新的过期日期
     */
    public void changeExpirationDate(V dataVersion, LocalDate expirationDate) {
        final LocalDate oldExpirationDate = dataVersion.getExpirationDate();
        dataVersion.setExpirationDate(expirationDate);
        dataVersion.setChanged(true);
        refreshVersionExpirationDateInChain(dataVersion, oldExpirationDate);
    }

    /**
     * 分裂版本
     * <p>
     * 将一个版本从指定日期分裂为两个版本：
     * <ul>
     *     <li>原版本的过期日期调整为分裂日期前一天</li>
     *     <li>新版本从分裂日期开始，继承原版本的过期日期</li>
     * </ul>
     * </p>
     *
     * @param effectiveVersion 要分裂的版本
     * @param splitDate        分裂日期
     *
     * @return 分裂后的新版本
     */
    protected V splitVersion(V effectiveVersion, LocalDate splitDate) {
        final V nextVersion = effectiveVersion.splitNextVersion(splitDate);
        changeExpirationDate(effectiveVersion, DateUtil.yesterday(splitDate));
        addVersionToChain(nextVersion);
        return nextVersion;
    }

    /**
     * 内部移动版本实现
     * <p>创建新版本并移除原版本</p>
     *
     * @param targetVersion 要移动的版本
     * @param targetDate    目标日期
     */
    protected void moveVersion(V targetVersion, LocalDate targetDate) {
        final V clone = targetVersion.splitNextVersion(targetDate);
        changeVersionDate(clone, targetDate);
        addVersionToChain(clone);
        removeVersionInChain(targetVersion);
    }

    /**
     * 清除版本链中的所有版本
     * <p>将所有版本移至已移除列表，并清空索引映射</p>
     */
    protected void clearAllElements() {
        this.versionDateTreeMap.forEach((date, version) -> {
            version.setRemoved(true);
            this.removeVersions.add(new AloneReleaseDateVersion<>(version.getReleaseDateVersionMetaData(), version.getBusinessData()));
        });
        this.versionDateTreeMap.clear();
        this.expirationDateTreeMap.clear();
    }

    @Override
    public List<V> findVersionsInWindowN(LocalDate versionDate, int n) {
        List<V> result = new ArrayList<>(n * 2 + 1);

        // 获取前面n个元素
        Map.Entry<LocalDate, V> lowerEntry = this.versionDateTreeMap.lowerEntry(versionDate);
        if (lowerEntry != null) {
            // 使用降序迭代器获取前面的n个元素
            var descendingIterator = this.versionDateTreeMap.headMap(versionDate, false).descendingMap().entrySet().iterator();
            List<V> previousVersions = new ArrayList<>(n);
            int count = 0;
            while (descendingIterator.hasNext() && count < n) {
                previousVersions.add(descendingIterator.next().getValue());
                count++;
            }
            result.addAll(previousVersions);
        }

        // 添加当前元素（如果存在）
        V currentVersion = this.versionDateTreeMap.get(versionDate);
        if (currentVersion != null) {
            result.add(currentVersion);
        }

        // 获取后面n个元素
        var tailMap = this.versionDateTreeMap.tailMap(versionDate, false);
        var iterator = tailMap.entrySet().iterator();
        int count = 0;
        while (iterator.hasNext() && count < n) {
            result.add(iterator.next().getValue());
            count++;
        }

        return result;
    }

    @Override
    public List<V> findVersionsInWindowN(AbstractReleaseDateVersion<D, V> version, int n) {
        return findVersionsInWindowN(version.getVersionDate(), n);
    }

    @Override
    public LocalDate getGenesisDate() {
        return CollUtil.isNotEmpty(this.versionDateTreeMap) ? this.versionDateTreeMap.firstKey() : null;
    }

    @Override
    public Optional<LocalDate> getDeletedDate() {
        return Optional.ofNullable(this.versionDateTreeMap.lastEntry())
                .map(entry -> {
                    if (Objects.nonNull(entry.getValue()) && entry.getValue().isDeleted()) {
                        return entry.getKey();
                    } else {
                        return null;
                    }
                });
    }

    @Override
    public Optional<V> findEffectiveVersion(LocalDate queryDate) {
        return Optional.ofNullable(this.versionDateTreeMap.floorEntry(queryDate))
                .map(Map.Entry::getValue);
    }

    @Override
    public Optional<V> findVersionByVersionDateEq(LocalDate versionDate) {
        return Optional.ofNullable(this.versionDateTreeMap.get(versionDate));
    }

    @Override
    public Optional<V> findVersionByExpirationDateEq(LocalDate expirationDate) {
        return Optional.ofNullable(this.expirationDateTreeMap.get(expirationDate));
    }

    @Override
    public Optional<V> findPrecedingVersion(V dateVersion) {
        return Optional.ofNullable(this.versionDateTreeMap.lowerEntry(dateVersion.getVersionDate())).map(Map.Entry::getValue);
    }

    @Override
    public Optional<V> findFollowingVersion(V dataVersion) {
        return Optional.ofNullable(this.versionDateTreeMap.higherEntry(dataVersion.getVersionDate())).map(Map.Entry::getValue);
    }

    @Override
    public Collection<V> getAllVersions() {
        return this.versionDateTreeMap.values();
    }

    @Override
    public Collection<V> getChangedVersions() {
        final var elements = this.versionDateTreeMap.values();
        if (CollUtil.isEmpty(elements)) {
            return List.of();
        }
        return elements.stream().filter(AbstractReleaseDateVersion::isChanged).toList();
    }

    @Override
    public List<AloneReleaseDateVersion<D>> getRemovedVersions() {
        return this.removeVersions;
    }

    @Override
    public Collection<V> getBehindVersions(V current) {
        return this.versionDateTreeMap.tailMap(current.getVersionDate(), false).values();
    }

    @Override
    public Collection<V> getPrecedingVersions(V current) {
        return this.versionDateTreeMap.headMap(current.getVersionDate(), false).values();
    }

    @Override
    public Collection<V> getVersionsByVersionDateBetween(LocalDate startDate, LocalDate endDate) {
        return this.versionDateTreeMap.subMap(startDate, true, endDate, true).values();
    }

    @Override
    public Collection<V> getVersionsByExpirationDateBetween(LocalDate startDate, LocalDate endDate) {
        return this.expirationDateTreeMap.subMap(startDate, true, endDate, true).values();
    }

    @Override
    public Collection<V> getVersionsByRangeCross(LocalDate startDate, LocalDate endDate) {
        if (CollUtil.isEmpty(this.versionDateTreeMap.values())) {
            return List.of();
        }
        return this.versionDateTreeMap.values().stream().filter(e -> DateUtil.noAfter(e.getVersionDate(), endDate)
                && DateUtil.noBefore(e.getExpirationDate(), startDate)).toList();
    }

    @Override
    public boolean haveVersionInRangeDate(LocalDate startDate, LocalDate endDate) {
        return !this.getVersionsByVersionDateBetween(startDate, endDate).isEmpty();
    }

    @Override
    public Result<Void, WrappedError> modify(LocalDate modifyDate, BusinessDataModifyInterface modifyModel) {
        LogUtil.debug("Modifying version chain, dataId={}, modifyDate={}", getDataId(), modifyDate);
        Optional<V> effectiveDataVersionOpt = findEffectiveVersion(modifyDate);
        if (effectiveDataVersionOpt.isEmpty()) {
            return Result.err(WrappedError.of(ReleaseDateVersionErrorType.DATA_VERSION_NOT_EXIST));
        }
        V effectiveDataVersion = effectiveDataVersionOpt.get();
        LocalDate effectiveVersionDate = effectiveDataVersion.getVersionDate();
        // 是否同一天，需要进行版本合并
        if (DateUtil.isSameDay(modifyDate, effectiveVersionDate)) {
            // 合并变更
            effectiveDataVersion.modifyBusinessData(modifyModel);
            if (ReleaseVersionType.checkTransferValid(effectiveDataVersion.getReleaseVersionType(), ReleaseVersionType.MODIFY)) {
                effectiveDataVersion.setReleaseVersionType(ReleaseVersionType.MODIFY);
            }
        } else {
            final V postModifiedVersion = splitVersion(effectiveDataVersion, modifyDate);
            postModifiedVersion.modifyBusinessData(modifyModel);
            postModifiedVersion.setReleaseVersionType(ReleaseVersionType.MODIFY);
        }
        effectiveDataVersion.setChanged(true);
        return Result.ok();
    }

    @Override
    public Result<Void, WrappedError> delete(LocalDate deleteDate) {
        // 数据是否存在
        LogUtil.debug("Delete data, dataId={}, deleteDate={}", getDataId(), deleteDate);
        Optional<V> effectiveDataVersionOpt = findEffectiveVersion(deleteDate);
        if (effectiveDataVersionOpt.isEmpty()) {
            return Result.err(WrappedError.of(ReleaseDateVersionErrorType.DATA_VERSION_NOT_EXIST));
        }
        // 数据版本是否已被删除
        V effectiveDataVersion = effectiveDataVersionOpt.get();
        if (effectiveDataVersion.isDeleted()) {
            return Result.err(WrappedError.of(ReleaseDateVersionErrorType.DATA_VERSION_HAD_BEEN_DELETED));
        }
        // 删除创建版本特殊处理
        if (DateUtil.isSameDay(deleteDate, this.getGenesisDate())) {
            this.clearAllElements();
            return Result.ok();
        }
        // 删除数据版本是否需要合并
        LocalDate effectiveVersionDate = effectiveDataVersion.getVersionDate();
        Collection<V> behindDataVersions = getBehindVersions(effectiveDataVersion);
        final var removedVersions = new ArrayList<>(behindDataVersions);
        if (DateUtil.isSameDay(deleteDate, effectiveVersionDate)) {
            // 合并
            effectiveDataVersion.setDeleted(true);
            if (ReleaseVersionType.checkTransferValid(effectiveDataVersion.getReleaseVersionType(), ReleaseVersionType.DELETE)) {
                effectiveDataVersion.setReleaseVersionType(ReleaseVersionType.DELETE);
            }
        } else {
            // 分裂
            final V deletedVersion = splitVersion(effectiveDataVersion, deleteDate);
            deletedVersion.setDeleted(true);
            deletedVersion.setReleaseVersionType(ReleaseVersionType.DELETE);
        }
        effectiveDataVersion.setChanged(true);
        // 移除
        for (V behindDataVersion : removedVersions) {
            final Result<Void, WrappedError> result = this.remove(behindDataVersion);
            if (!result.isOk()) {
                return result;
            }
        }
        return Result.ok();
    }

    @Override
    public Result<Void, WrappedError> move(V originVersion, LocalDate targetDate) {
        final var followingVersionOpt = findFollowingVersion(originVersion);
        if (followingVersionOpt.isPresent()) {
            final var followingVersion = followingVersionOpt.get();
            if (DateUtil.noBefore(targetDate, followingVersion.getVersionDate())) {
                return Result.err(WrappedError.of(ReleaseDateVersionErrorType.MOVE_DATE_MUST_NO_CROSS_EXISTS_VERSION));
            }
        }
        final var precedingVersionOpt = findPrecedingVersion(originVersion);
        if (precedingVersionOpt.isPresent()) {
            final var precedingVersion = precedingVersionOpt.get();
            if (DateUtil.noAfter(targetDate, precedingVersion.getVersionDate())) {
                return Result.err(WrappedError.of(ReleaseDateVersionErrorType.MOVE_DATE_MUST_NO_CROSS_EXISTS_VERSION));
            }
            changeExpirationDate(precedingVersion, targetDate.minusDays(1));
        }
        moveVersion(originVersion, targetDate);
        return Result.ok();
    }

    @Override
    public Result<Void, WrappedError> duelChangePoint(ReleaseDateProcessPoint<D> changePoint) {
        final LocalDate changeDate = changePoint.getReleaseDate();

        final Optional<V> targetDataVersionOpt = findEffectiveVersion(changeDate);
        if (targetDataVersionOpt.isEmpty()) {
            return Result.err(WrappedError.of(ReleaseDateVersionErrorType.DATA_VERSION_NOT_EXIST));
        }
        final V targetDataVersion = targetDataVersionOpt.get();
        final Consumer<D> changeConsumer = changePoint.getChangeConsumer();
        if ((DateUtil.isSameDay(targetDataVersion.getVersionDate(), changeDate))) {
            changeConsumer.accept(targetDataVersion.getBusinessData());
            if (ReleaseVersionType.checkTransferValid(targetDataVersion.getReleaseVersionType(), changePoint.getReleaseVersionType())) {
                targetDataVersion.setReleaseVersionType(changePoint.getReleaseVersionType());
            }
        } else {
            final V changedDataVersion = splitVersion(targetDataVersion, changeDate);
            changeConsumer.accept(changedDataVersion.getBusinessData());
            changedDataVersion.setReleaseVersionType(changePoint.getReleaseVersionType());
        }
        targetDataVersion.setChanged(true);
        return Result.ok();
    }

    @Override
    public Result<Void, WrappedError> remove(V removedVersion) {
        final Optional<V> precedingVersionOpt = findPrecedingVersion(removedVersion);
        if (precedingVersionOpt.isEmpty()) {
            return Result.ok();
        }
        final var precedingVersion = precedingVersionOpt.get();
        removeVersionInChain(removedVersion);
        changeExpirationDate(precedingVersion, removedVersion.getExpirationDate());
        return Result.ok();
    }
}
