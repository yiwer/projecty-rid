package com.github.ryan.facility.error;


import java.text.MessageFormat;


/**
 * <b>错误类型接口</b>
 * <p>
 * 定义错误信息的标准接口，所有错误类型枚举应实现此接口。
 * 用于统一错误码和错误消息的定义，便于错误处理和国际化。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *     <li><b>模块隔离</b>：通过 {@link #getModule()} 区分错误来源模块</li>
 *     <li><b>唯一标识</b>：{@code module + code} 组合保证全局唯一</li>
 *     <li><b>i18n友好</b>：通过 {@link #getMessageKey()} 支持国际化</li>
 *     <li><b>参数化消息</b>：通过 {@link #format(Object...)} 支持动态参数</li>
 * </ul>
 *
 * <h3>错误码规范：</h3>
 * <pre>
 * 模块前缀（3位） + 错误序号（3位）
 * 例如：FACILITY 模块 = 500xxx
 *       USER 模块     = 100xxx
 *       ORDER 模块    = 200xxx
 * </pre>
 *
 * @author yvvb
 * @see FacilityErrorType
 * @since 2025/4/17
 */
public interface ErrorTypeInterface {

    // ==================== 核心属性 ====================

    /**
     * 获取模块标识
     * <p>用于区分错误来源模块，建议使用大写字母</p>
     *
     * @return 模块标识，如 "FACILITY", "USER", "ORDER"
     */
    default String getModule() {
        return "UNKNOWN";
    }

    /**
     * 获取错误码
     * <p>在模块内唯一，建议使用模块前缀+序号的方式</p>
     *
     * @return 错误码，用于唯一标识错误类型
     */
    int getCode();

    /**
     * 获取 i18n 消息键
     * <p>用于从 MessageSource 获取本地化消息</p>
     *
     * @return 消息键，格式建议：{module}.{error_name}
     */
    String getMessageKey();

    /**
     * 获取默认错误消息（未配置 i18n 时使用）
     * <p>支持 {@link MessageFormat} 占位符，如 "用户 {0} 不存在"</p>
     *
     * @return 默认错误消息模板
     */
    String getDefaultMessage();

    // ==================== 派生方法 ====================

    /**
     * 获取完整错误码
     * <p>格式：MODULE-CODE，如 FACILITY-5001</p>
     *
     * @return 完整错误码
     */
    default String getFullCode() {
        return getModule() + "-" + getCode();
    }

    /**
     * 格式化错误消息（使用默认消息模板）
     *
     * @param args 消息参数
     *
     * @return 格式化后的消息
     */
    default String format(Object... args) {
        if (args == null || args.length == 0) {
            return getDefaultMessage();
        }
        return MessageFormat.format(getDefaultMessage(), args);
    }

}
