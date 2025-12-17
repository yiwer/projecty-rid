package com.github.ryan.version.release_date;


import com.github.ryan.version.core.BusinessData;
import com.github.ryan.version.release_date.chain.ReleaseDateVersionChain;
import com.github.ryan.version.release_date.version.AbstractReleaseDateVersion;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <b>数据版本链变更事件抽象类</b>
 * <p>
 * 基于Spring事件机制，用于发布数据版本链变更的事件。
 * 当版本链发生新增、修改、删除等操作时，可以发布对应的事件。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>版本链创建后通知其他模块</li>
 *     <li>版本链修改后触发后续处理</li>
 *     <li>异步处理版本链变更</li>
 * </ul>
 *
 * @param <D> 聚合数据类型
 * @param <V> 版本类型
 * @param <C> 版本链类型
 *
 * @author yvvb
 * @see ApplicationEvent
 * @see ReleaseDateVersionChain
 * @since 2025/5/5
 */
public abstract class ReleaseDateVersionChainEvent<
        D extends BusinessData<D>,
        V extends AbstractReleaseDateVersion<D, V>,
        C extends ReleaseDateVersionChain<D, V>
        > extends ApplicationEvent {

    /**
     * 发生变更的版本链
     */
    @Getter
    private final C versionChain;

    /**
     * 构造函数
     *
     * @param versionChain 发生变更的版本链
     */
    public ReleaseDateVersionChainEvent(C versionChain) {
        super(versionChain);
        this.versionChain = versionChain;
    }
}
