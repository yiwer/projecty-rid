package com.github.ryan.version.release_date;

import com.github.ryan.facility.error.ErrorTypeInterface;
import lombok.Getter;

/**
 * <b>日期版本模块错误类型枚举</b>
 * <p>
 * 定义日期版本管理模块中可能出现的错误类型。
 * 实现 {@link ErrorTypeInterface} 接口，支持统一的错误处理机制。
 * </p>
 *
 * <h3>错误码范围：</h3>
 * <p>5200 - 5299 为日期版本模块保留</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * Result<Void, WrappedError> result = versionChain.modify(date, model);
 * if (!result.isOk()) {
 *     WrappedError error = result.getErr();
 *     // error.getErrorType() 可能是 DATA_VERSION_NOT_EXIST
 * }
 * }</pre>
 *
 * @author yvvb
 * @see com.github.ryan.facility.error.WrappedError
 * @since 11/5/2025
 */
@Getter
public enum ReleaseDateVersionErrorType implements ErrorTypeInterface {
    /**
     * 日期对应的数据版本不存在
     */
    DATA_VERSION_NOT_EXIST(5200, "date_version.data_version_not_exist", "日期对应的数据版本不存在"),

    /**
     * 日期对应的数据版本已被删除
     */
    DATA_VERSION_HAD_BEEN_DELETED(5201, "date_version.data_version_had_been_deleted", "日期对应的数据版本已被删除"),

    /**
     * 数据版本移动的目标日期不能早于当前日期
     */
    MOVE_TARGET_DATE_MUST_NO_BEFORE_TODAY(5202, "date_version.move_target_date_must_no_before_today", "数据版本移动的目标日期不能早于当前日期"),

    /**
     * 数据版本移动不能跨越已有的数据版本
     */
    MOVE_DATE_MUST_NO_CROSS_EXISTS_VERSION(5203, "date_version.move_date_must_no_cross_exists_version", "数据版本移动不能跨越已有的数据版本"),

    /**
     * 生成修改版本日历失败
     */
    GENERATE_MODIFY_CALENDER_ERROR(5204, "date_version.generate_modify_calender_error", "生成修改版本日历失败"),

    /**
     * 数据异常：存在多个创建版本
     */
    EXISTS_MULTI_GENESIS_VERSION(5205, "date_version.exists_multi_genesis_version", "数据异常：存在多个创建版本"),

    /**
     * 数据异常：数据版本被多次删除
     */
    EXISTS_MULTI_DELETE_VERSION(5206, "date_version.exists_multi_delete_version", "数据异常：数据版本被多次删除"),
    ;

    /**
     * 模块标识符
     */
    private static final String MODULE = "RELEASE_DATE_VERSION";

    // ======================== 实例属性 ========================

    /**
     * 错误码
     */
    private final int code;

    /**
     * i18n 消息键
     */
    private final String messageKey;

    /**
     * 默认错误消息（支持 MessageFormat 占位符）
     */
    private final String defaultMessage;

    // ======================== 构造函数 ========================

    /**
     * 构造函数
     *
     * @param code           错误码
     * @param messageKey     i18n 消息键
     * @param defaultMessage 默认错误消息
     */
    ReleaseDateVersionErrorType(int code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }
}
