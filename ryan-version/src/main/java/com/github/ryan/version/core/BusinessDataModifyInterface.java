package com.github.ryan.version.core;

/**
 * <b>业务数据修改模型标记接口</b>
 * <p>
 * 标记接口，用于标识修改业务数据所需的输入模型。
 * 实现类应包含修改业务数据时需要更新的字段。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义修改模型
 * public class StopModifyModel implements BusinessDataModifyInterface {
 *     private String stopName;
 *     private String address;
 *
 *     // getters and setters...
 * }
 *
 * // 在业务数据中实现修改逻辑
 * public class StopData implements BusinessData<StopData> {
 *     @Override
 *     public void modifyData(BusinessDataModifyInterface modifyModel) {
 *         if (modifyModel instanceof StopModifyModel model) {
 *             if (model.getStopName() != null) {
 *                 this.stopName = model.getStopName();
 *             }
 *             if (model.getAddress() != null) {
 *                 this.address = model.getAddress();
 *             }
 *         }
 *     }
 * }
 *
 * // 在版本链中使用
 * StopModifyModel modifyModel = new StopModifyModel();
 * modifyModel.setStopName("新站名");
 * versionChain.modify(LocalDate.now(), modifyModel);
 * }</pre>
 *
 * @author yvvb
 * @see BusinessData#modifyData(BusinessDataModifyInterface)
 * @see ReleaseDateVersionChain#modify(LocalDate, BusinessDataModifyInterface)
 * @since 11/5/2025
 */
public interface BusinessDataModifyInterface {
}
