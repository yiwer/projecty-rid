package com.github.ryan.version;


import com.github.ryan.facility.FacilityModule;
import com.github.ryan.facility.log.LogUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * <b>版本管理模块配置类</b>
 * <p>
 * 版本管理模块的 Spring 配置入口。
 * 自动扫描并注册 version 包下的所有组件。
 * </p>
 *
 * <h3>模块功能：</h3>
 * <ul>
 *     <li>基于日期的版本链管理</li>
 *     <li>版本的懒加载机制</li>
 *     <li>版本操作（新增、修改、删除、移动）</li>
 *     <li>版本链事件发布</li>
 * </ul>
 *
 * <h3>依赖模块：</h3>
 * <p>依赖 {@link FacilityModule} 提供基础设施支持</p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * @SpringBootApplication
 * @Import(VersionModule.class)
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }</pre>
 *
 * @author yvvb
 * @see FacilityModule
 * @since 11/5/2025
 */
@Configuration
@AutoConfigureOrder(value = 2)
@ComponentScan(basePackages = "com.github.ryan.version",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {VersionModule.class}))
@Import(FacilityModule.class)
public class VersionModule {

    /**
     * 模块初始化方法
     * <p>在 Spring 容器初始化完成后调用，记录模块加载信息</p>
     */
    @PostConstruct
    public void moduleInit() {
        LogUtil.info("init module:{},order:{}", VersionModule.class.getSimpleName(), 2);
    }
}
