package com.github.ryan.version.release_date.service;

import java.time.LocalDate;

/**
 * <b>数据版本服务接口</b>
 * <p>
 * 定义数据版本的基本操作，包括移除版本、移动版本等。
 * 不同类型的数据需要实现该接口来处理各自的版本管理逻辑。
 * </p>
 *
 * @author yvvb
 * @since 2025/4/29
 */
public interface IDataVersionService {

    /**
     * 获取服务处理的数据类型编码
     * <p>
     * 用于标识该服务处理哪种类型的数据版本
     * </p>
     *
     * @return 数据类型编码
     */
    Integer getDataType();

    /**
     * 移除指定日期的数据版本
     * <p>
     * 从版本链中移除指定数据在指定日期的版本
     * </p>
     *
     * @param dataId      数据ID
     * @param versionDate 要移除的版本日期
     */
    void removeVersion(Long dataId, LocalDate versionDate);

    /**
     * 移动数据版本到目标日期
     * <p>
     * 将指定数据的某个版本从原日期移动到目标日期
     * </p>
     *
     * @param dataId      数据ID
     * @param versionDate 原版本日期
     * @param targetDate  目标日期
     */
    void moveVersion(Long dataId, LocalDate versionDate, LocalDate targetDate);
}
