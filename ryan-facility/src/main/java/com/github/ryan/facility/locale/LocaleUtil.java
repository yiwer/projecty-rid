package com.github.ryan.facility.locale;

import com.github.ryan.facility.common.CommonUtil;
import com.github.ryan.facility.context.SpringContextHolder;
import lombok.experimental.UtilityClass;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * <b>国际化工具类</b>
 * <p>
 * 提供基于Spring MessageSource的国际化消息翻译功能。
 * 支持带参数的消息翻译和自动获取当前线程的Locale。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 简单翻译
 * String msg = LocaleUtil.translateMessage("user.not_exist");
 *
 * // 带参数的翻译
 * String msg = LocaleUtil.translateMessageWithArgs("user.welcome", new Object[]{username});
 *
 * // 指定Locale的翻译
 * String msg = LocaleUtil.translateMessage("user.not_exist", Locale.ENGLISH);
 * }</pre>
 *
 * @author yvvb
 * @since 2025/5/4
 */
@UtilityClass
public class LocaleUtil {

    /**
     * <b>获取当前线程的Locale</b>
     * <p>
     * 从Spring的LocaleContextHolder中获取当前请求的Locale。
     * </p>
     *
     * @return {@link Locale} 当前线程的Locale
     */
    public static Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * <b>带参数的消息翻译（指定Locale）</b>
     *
     * @param messageKey  消息键
     * @param messageArgs 消息参数数组
     * @param locale      {@link Locale} 目标语言环境
     *
     * @return 翻译后的消息，如果翻译失败则返回原始key
     */
    public static String translateMessageWithArgs(String messageKey, Object[] messageArgs, Locale locale) {
        if (CommonUtil.isBlank(messageKey)) {
            return "";
        }
        return SpringContextHolder.getBean(MessageSource.class).map(messageSource ->
                messageSource.getMessage(messageKey, messageArgs, locale)
        ).orElse(messageKey);
    }

    /**
     * <b>简单消息翻译（指定Locale）</b>
     *
     * @param messageKey 消息键
     * @param locale     {@link Locale} 目标语言环境
     *
     * @return 翻译后的消息，如果翻译失败则返回原始key
     */
    public static String translateMessage(String messageKey, Locale locale) {
        if (CommonUtil.isBlank(messageKey)) {
            return "";
        }
        return SpringContextHolder.getBean(MessageSource.class).map(messageSource ->
                messageSource.getMessage(messageKey, null, locale)
        ).orElse(messageKey);
    }

    /**
     * <b>带参数的消息翻译（使用当前线程Locale）</b>
     *
     * @param messageKey  消息键
     * @param messageArgs 消息参数数组
     *
     * @return 翻译后的消息，如果翻译失败则返回原始key
     */
    public static String translateMessageWithArgs(String messageKey, Object[] messageArgs) {
        if (CommonUtil.isBlank(messageKey)) {
            return "";
        }
        return SpringContextHolder.getBean(MessageSource.class).map(messageSource ->
                messageSource.getMessage(messageKey, messageArgs, getLocale())
        ).orElse(messageKey);
    }

    /**
     * <b>简单消息翻译（使用当前线程Locale）</b>
     *
     * @param messageKey 消息键
     *
     * @return 翻译后的消息，如果翻译失败则返回原始key
     */
    public static String translateMessage(String messageKey) {
        if (CommonUtil.isBlank(messageKey)) {
            return "";
        }
        return SpringContextHolder.getBean(MessageSource.class).map(messageSource ->
                messageSource.getMessage(messageKey, null, getLocale())
        ).orElse(messageKey);
    }
}
