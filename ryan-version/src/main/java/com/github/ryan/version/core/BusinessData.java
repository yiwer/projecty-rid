package com.github.ryan.version.core;

import com.github.ryan.facility.copy.CopyTrait;

/**
 * <b>业务数据接口</b>
 * <p>
 * 定义版本管理中业务数据的核心契约。所有需要进行版本管理的业务实体都应实现此接口。
 * 继承 {@link CopyTrait} 以支持深拷贝，用于版本分裂时复制业务数据。
 * </p>
 *
 * <h3>核心能力：</h3>
 * <ul>
 *     <li>数据标识：通过 dataId 和 dataType 唯一标识业务数据</li>
 *     <li>深拷贝：支持版本分裂时的数据复制</li>
 *     <li>数据修改：支持通过修改模型更新数据</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class StopData implements BusinessData<StopData> {
 *     private Long stopId;
 *     private String stopName;
 *     private String address;
 *
 *     @Override
 *     public Long getDataId() {
 *         return stopId;
 *     }
 *
 *     @Override
 *     public Integer getDataType() {
 *         return DataType.STOP.getCode();  // 例如：1
 *     }
 *
 *     @Override
 *     public StopData copy() {
 *         StopData copy = new StopData();
 *         copy.stopId = this.stopId;
 *         copy.stopName = this.stopName;
 *         copy.address = this.address;
 *         return copy;
 *     }
 *
 *     @Override
 *     public void modifyData(BusinessDataModifyInterface modifyModel) {
 *         if (modifyModel instanceof StopModifyModel model) {
 *             this.stopName = model.getStopName();
 *             this.address = model.getAddress();
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <D> 业务数据的具体类型（自引用泛型，用于 CopyTrait）
 *
 * @author yvvb
 * @see CopyTrait
 * @see BusinessDataModifyInterface
 * @since 11/5/2025
 */
public interface BusinessData<D extends BusinessData<D>> extends CopyTrait<D> {

    /**
     * 未知数据名称的默认值
     */
    String UNKNOWN_DATA_NAME = "unknown";

    /**
     * 获取数据ID
     * <p>
     * 数据的唯一业务标识，与 dataType 共同确定一条业务数据
     * </p>
     *
     * @return 数据ID
     */
    Long getDataId();

    /**
     * 获取数据类型
     * <p>
     * 用于区分不同类型的业务数据，如站点(1)、线路(2)等
     * </p>
     *
     * @return 数据类型编码
     */
    Integer getDataType();

    /**
     * 获取数据名称
     * <p>
     * 用于日志、调试等场景的可读名称
     * </p>
     *
     * @return 数据名称，默认返回 "unknown"
     */
    default String getDataName() {
        return UNKNOWN_DATA_NAME;
    }

    /**
     * 修改业务数据
     * <p>
     * 使用修改模型更新当前业务数据。子类需要重写此方法以实现具体的修改逻辑。
     * </p>
     *
     * @param modifyModel 修改模型，包含要修改的字段值
     *
     * @throws UnsupportedOperationException 如果子类未实现此方法
     */
    default void modifyData(BusinessDataModifyInterface modifyModel) {
        throw new UnsupportedOperationException("not support modify data");
    }

}
