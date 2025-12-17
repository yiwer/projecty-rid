package com.github.ryan.version.release_date.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>数据版本服务策略类</b>
 * <p>
 * 提供根据数据类型获取对应版本服务的策略模式实现。
 * 支持的数据类型包括：站点(1)、线路(2)、站牌(3)等。
 * </p>
 * <p>
 * 工作原理：
 * <ul>
 *   <li>通过Spring依赖注入收集所有{@link IDataVersionService}实现</li>
 *   <li>根据每个服务的dataType构建映射关系</li>
 *   <li>提供静态方法根据数据类型获取对应服务</li>
 * </ul>
 * </p>
 *
 * @author yvvb
 * @since 2025/4/29
 */
@Component
public class DataVersionServiceStrategy {
    /**
     * 数据类型与版本服务的映射关系
     * <p>使用ConcurrentHashMap保证线程安全</p>
     */
    private static final Map<Integer, IDataVersionService> dataVersionServiceMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，初始化服务映射
     *
     * @param dataVersionServices Spring注入的所有版本服务实现
     */
    public DataVersionServiceStrategy(List<IDataVersionService> dataVersionServices) {
        synchronized (DataVersionServiceStrategy.class) {
            dataVersionServiceMap.clear();
            initMap(dataVersionServices);
        }
    }

    /**
     * 根据数据类型获取对应的版本服务
     *
     * @param dataType 数据类型编码
     *
     * @return 对应的版本服务，如果不存在则返回Optional.empty()
     */
    public static Optional<IDataVersionService> getDataVersionService(Integer dataType) {
        return Optional.ofNullable(dataVersionServiceMap.get(dataType));
    }

    /**
     * 初始化数据类型与服务的映射关系
     */
    private void initMap(List<IDataVersionService> dataVersionServices) {
        for (IDataVersionService dataVersionService : dataVersionServices) {
            dataVersionServiceMap.putIfAbsent(dataVersionService.getDataType(), dataVersionService);
        }
    }
}
