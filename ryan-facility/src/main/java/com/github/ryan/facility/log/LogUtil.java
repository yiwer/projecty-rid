package com.github.ryan.facility.log;

import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.map.WeakConcurrentMap;
import cn.hutool.core.map.reference.WeakKeyConcurrentMap;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.ryan.facility.context.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


/**
 * <b>日志输出工具类</b>
 * <p>
 * 提供统一的日志输出接口，支持TRACE/DEBUG/INFO/WARN/ERROR等日志级别。
 * 该工具类具有以下特性：
 * </p>
 * <ul>
 *     <li>自动获取调用者的类名作为Logger名称</li>
 *     <li>使用弱引用缓存Logger实例，避免内存泄漏</li>
 *     <li>支持日志后处理器，可扩展日志上报等功能</li>
 *     <li>支持占位符格式化消息</li>
 *     <li>缓存后处理器实例，提高性能</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * LogUtil.info("用户{}登录成功", username);
 * LogUtil.error(exception, "处理订单{}失败", orderId);
 * }</pre>
 *
 * @author yvvb
 * @since 2025/4/17
 */
public final class LogUtil {

    /**
     * 私有构造函数，防止实例化
     */
    private LogUtil() {
    }

    /**
     * Logger实例缓存，使用弱引用缓存避免内存泄漏
     */
    private static final Map<String, Logger> LOGGER_CACHE = new WeakKeyConcurrentMap<>();

    /**
     * 缓存的日志后处理器组合器
     */
    private static final AtomicReference<LogPostHandlerComposite> HANDLER_CACHE = new AtomicReference<>();

    /**
     * <b>动态设置指定类的日志级别</b>
     * <p>
     * 在运行时动态调整某个类的日志输出级别，便于调试问题。
     * </p>
     *
     * @param clazz {@link Class} 需要设置日志级别的类
     * @param level {@link Level} 目标日志级别（DEBUG/INFO/WARN/ERROR等）
     */
    public static void setLevel(Class<?> clazz, Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(clazz);
        logger.setLevel(ch.qos.logback.classic.Level.convertAnSLF4JLevel(level));
    }

    /**
     * <b>输出TRACE级别日志</b>
     *
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void trace(String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.TRACE)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.trace(msg);
            invokePostHandler(msg, Level.TRACE, callerClassName, null);
        }
    }

    /**
     * <b>输出DEBUG级别日志</b>
     *
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void debug(String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.DEBUG)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.debug(msg);
            invokePostHandler(msg, Level.DEBUG, callerClassName, null);
        }
    }

    /**
     * <b>输出INFO级别日志</b>
     *
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void info(String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.INFO)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.info(msg);
            invokePostHandler(msg, Level.INFO, callerClassName, null);
        }
    }

    /**
     * <b>输出WARN级别日志</b>
     *
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void warn(String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.WARN)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.warn(msg);
            invokePostHandler(msg, Level.WARN, callerClassName, null);
        }
    }

    /**
     * <b>输出WARN级别日志（带异常信息）</b>
     *
     * @param t       {@link Throwable} 异常对象
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void warn(Throwable t, String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.WARN)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.warn(msg, t);
            invokePostHandler(msg, Level.WARN, callerClassName, t);
        }
    }

    /**
     * <b>输出ERROR级别日志</b>
     *
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void error(String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.ERROR)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.error(msg);
            invokePostHandler(msg, Level.ERROR, callerClassName, null);
        }
    }

    /**
     * <b>输出ERROR级别日志（带异常信息）</b>
     *
     * @param t       {@link Throwable} 异常对象
     * @param msgTemp 日志消息模板，支持{}占位符
     * @param args    占位符参数
     */
    public static void error(Throwable t, String msgTemp, Object... args) {
        final var callerClassName = getCallerClassName();
        final var logger = getLogger(callerClassName);
        if (checkIsEnableForLevel(logger, Level.ERROR)) {
            final var msg = CharSequenceUtil.format(msgTemp, args);
            logger.error(msg, t);
            invokePostHandler(msg, Level.ERROR, callerClassName, t);
        }
    }

    /**
     * <b>调用日志后处理器</b>
     * <p>
     * 缓存后处理器实例，避免每次调用都获取Bean。
     * </p>
     *
     * @param message        日志消息
     * @param level          日志级别
     * @param callerClassName 调用者类名
     * @param throwable      异常对象（可为null）
     */
    private static void invokePostHandler(String message, Level level, String callerClassName, Throwable throwable) {
        LogPostHandlerComposite handler = HANDLER_CACHE.get();
        if (handler == null) {
            SpringContextHolder.getBean(LogPostHandlerComposite.class).ifOk(h -> {
                HANDLER_CACHE.compareAndSet(null, h);
                doInvokePostHandler(h, message, level, callerClassName, throwable);
            });
        } else {
            doInvokePostHandler(handler, message, level, callerClassName, throwable);
        }
    }

    /**
     * <b>执行后处理器调用</b>
     */
    private static void doInvokePostHandler(LogPostHandlerComposite handler, String message, Level level, String callerClassName, Throwable throwable) {
        LogContext context = LogContext.builder()
                .message(message)
                .level(level)
                .callerClassName(callerClassName)
                .throwable(throwable)
                .build();
        handler.handle(context);
    }

    /**
     * <b>获取调用者的类名</b>
     * <p>
     * 通过分析当前线程的调用栈，获取调用LogUtil方法的业务代码类名。
     * 调用链：getStackTrace <- getCallerClassName <- error/info等方法 <- 业务代码
     * </p>
     *
     * @return 调用者的完整类名
     */
    private static String getCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 调用链：getStackTrace<-getCallerClassName <- error/info等方法 <- 业务代码
        if (stackTrace.length >= 4) {
            return stackTrace[3].getClassName();
        }
        return LogUtil.class.getName();
    }

    /**
     * <b>获取或创建Logger实例</b>
     * <p>
     * 使用弱引用缓存Logger实例，避免重复创建。
     * </p>
     *
     * @param className 类名
     * @return {@link Logger} 日志器实例
     */
    private static Logger getLogger(String className) {
        return LOGGER_CACHE.computeIfAbsent(className, LoggerFactory::getLogger);
    }

    /**
     * <b>检查Logger是否启用指定级别</b>
     *
     * @param logger {@link Logger} 日志器实例
     * @param level  {@link Level} 日志级别
     * @return 是否启用该级别
     */
    private static boolean checkIsEnableForLevel(Logger logger, Level level) {
        return logger.isEnabledForLevel(level);
    }

    /**
     * <b>清除后处理器缓存</b>
     * <p>
     * 主要用于测试或Spring容器重启场景。
     * </p>
     */
    public static void clearHandlerCache() {
        HANDLER_CACHE.set(null);
    }
}
