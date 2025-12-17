package com.github.ryan.version.release_date;


import com.github.ryan.version.core.BusinessDataCreateInterface;
import com.github.ryan.version.release_date.chain.ReleaseDateVersionChain;

import java.util.List;

/**
 * <b>日期版本链工厂接口</b>
 * <p>
 * 用于创建数据版本链实例。
 * 不同类型的业务数据需要实现各自的工厂来创建对应的版本链。
 * </p>
 *
 * @param <M> 创建模型类型，包含创建版本链所需的数据
 * @param <C> 版本链类型
 *
 * @author yvvb
 * @see ReleaseDateVersionChain
 * @see AggregatedDataCreateModel
 * @since 11/5/2025
 */
public interface ReleaseDateVersionChainFactory<M extends BusinessDataCreateInterface, C extends ReleaseDateVersionChain<?, ?>> {

    /**
     * 创建数据版本链
     * <p>
     * 根据创建模型初始化一个新的数据版本链
     * </p>
     *
     * @param createdModel 创建模型，包含初始化版本链所需的数据
     *
     * @return 创建的版本链实例
     */
    C createDataVersionChain(M createdModel);

    /**
     * 批量创建数据版本链
     * <p>
     * 根据多个创建模型批量初始化版本链
     * </p>
     *
     * @param createdModels 创建模型列表
     *
     * @return 创建的版本链列表
     */
    default List<C> batchCreateDataVersionChain(List<M> createdModels) {
        return createdModels.stream().map(this::createDataVersionChain)
                .toList();
    }
}
