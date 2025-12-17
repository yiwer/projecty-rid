package com.github.ryan.version.core;

import com.github.ryan.version.release_date.ReleaseDateVersionChainFactory;

/**
 * <b>业务数据创建模型标记接口</b>
 * <p>
 * 标记接口，用于标识创建业务数据所需的输入模型。
 * 实现类应包含创建业务数据所需的所有字段。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class StopCreateModel implements BusinessDataCreateInterface {
 *     private String stopName;
 *     private String address;
 *     private LocalDate effectiveDate;
 *
 *     // getters and setters...
 * }
 *
 * // 在工厂中使用
 * public class StopVersionChainFactory
 *         implements ReleaseDateVersionChainFactory<StopCreateModel, StopVersionChain> {
 *
 *     @Override
 *     public StopVersionChain createDataVersionChain(StopCreateModel model) {
 *         StopData data = new StopData(model.getStopName(), model.getAddress());
 *         return new StopVersionChain(data, model.getEffectiveDate());
 *     }
 * }
 * }</pre>
 *
 * @author yvvb
 * @see ReleaseDateVersionChainFactory
 * @since 11/5/2025
 */
public interface BusinessDataCreateInterface {
}
