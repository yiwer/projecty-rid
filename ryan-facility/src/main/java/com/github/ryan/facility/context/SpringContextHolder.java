package com.github.ryan.facility.context;

import com.github.ryan.facility.error.FacilityErrorType;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.result.Result;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * <b>Spring上下文持有器</b>
 * <p>
 * 提供静态方式访问Spring容器中的Bean实例，便于在非 Spring 管理的类中获取 Spring Bean。
 * 实现 {@link ApplicationContextAware} 接口，在 Spring 容器初始化时自动注入 ApplicationContext。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 根据类型获取Bean
 * ComputeResult<UserService, WrappedError> result = SpringContextHolder.getBean(UserService.class);
 * result.ifOk(service -> service.doSomething());
 *
 * // 根据名称和类型获取Bean
 * SpringContextHolder.getBean("myService", MyService.class)
 *     .ifOk(service -> service.process());
 * }</pre>
 *
 * @author yvvb
 * @since 2025/4/17
 * @see Result
 * @see WrappedError
 */
@Component
@Getter
public class SpringContextHolder implements ApplicationContextAware {

    /**
     * Spring应用上下文
     */
    private static ApplicationContext applicationContext;

    /**
     * <b>检查ApplicationContext是否注入失败</b>
     *
     * @return true-未初始化，false-初始化
     */
    public static boolean isAwareFailed() {
        return applicationContext == null;
    }

    /**
     * <b>根据类型获取Spring Bean实例</b>
     *
     * @param clazz {@link Class} Bean的类型
     * @param <T>   Bean类型泛型
     * @return {@link com.github.ryan.facility.result.Result} 包含 Bean 实例或错误信息的计算结果
     */
    public static <T> Result<T, WrappedError> getBean(Class<T> clazz) {
        if (isAwareFailed()) {
            return Result.err(WrappedError.of(FacilityErrorType.CONTEXT_INSTANCE_NOT_INITIALIZED));
        }
        try {
            return Result.ok(applicationContext.getBean(clazz));
        } catch (BeansException exception) {
            return Result.err(WrappedError.of(FacilityErrorType.CONTEXT_GET_BEAN_ERROR, exception));
        }
    }

    /**
     * <b>根据名称和类型获取Spring Bean实例</b>
     *
     * @param beanName     Bean的名称
     * @param requiredType {@link Class} Bean的类型
     * @param <T>          Bean类型泛型
     * @return {@link Result} 包含 Bean 实例或错误信息的计算结果
     */
    public static <T> Result<T, WrappedError> getBean(String beanName, Class<T> requiredType) {
        if (isAwareFailed()) {
            return Result.err(WrappedError.of(FacilityErrorType.CONTEXT_INSTANCE_NOT_INITIALIZED));
        }
        try {
            return Result.ok(applicationContext.getBean(beanName, requiredType));
        } catch (BeansException exception) {
            return Result.err(WrappedError.of(FacilityErrorType.CONTEXT_GET_BEAN_ERROR, exception));
        }
    }

    /**
     * <b>Spring容器回调方法，注入ApplicationContext</b>
     *
     * @param applicationContext {@link ApplicationContext} Spring应用上下文
     * @throws BeansException Bean异常
     */
    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext) throws BeansException {
        if (applicationContext != null) {
            SpringContextHolder.applicationContext = applicationContext;
        }
    }
}
