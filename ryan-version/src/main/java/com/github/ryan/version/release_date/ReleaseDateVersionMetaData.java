package com.github.ryan.version.release_date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

/**
 * <b>日期版本元数据实体</b>
 * <p>
 * 存储版本的元数据信息，包括数据标识、版本日期、过期日期等。
 * 该类不包含实际的业务数据，仅作为版本的标识和状态信息。
 * </p>
 *
 * <h3>主要用途：</h3>
 * <ul>
 *     <li>版本唯一标识：通过 dataId + dataType + versionDate 唯一确定一个版本</li>
 *     <li>版本生命周期管理：通过 versionDate 和 expirationDate 定义版本有效期</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ReleaseDateVersionMetaData versionData = ReleaseDateVersionMetaData.builder()
 *     .dataId(1L)
 *     .dataType(DataType.STOP.getCode())
 *     .releaseVersionType(ReleaseVersionType.CREATE)
 *     .versionDate(LocalDate.of(2025, 1, 1))
 *     .expirationDate(LocalDate.of(9999, 12, 31))
 *     .build();
 * }</pre>
 *
 * @author yvvb
 * @see ReleaseVersionType
 * @since 11/5/2025
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseDateVersionMetaData {

    /**
     * 数据唯一标识
     * <p>业务数据的主键 ID</p>
     */
    private Long dataId;

    /**
     * 数据类型
     * <p>用于区分不同类型的业务数据，如站点(1)、线路(2)等</p>
     */
    private Integer dataType;

    /**
     * 版本类型
     * <p>表示该版本是创建、修改还是删除</p>
     */
    private ReleaseVersionType releaseVersionType;

    /**
     * 版本生效日期
     * <p>版本开始生效的日期（包含）</p>
     */
    private LocalDate versionDate;

    /**
     * 版本过期日期
     * <p>版本失效的日期（包含），不过期时通常设为 9999-12-31</p>
     */
    private LocalDate expirationDate;

    /**
     * 数据状态
     * <p>预留字段，用于扩展数据状态管理</p>
     */
    @Builder.Default
    private Short dataStatus = 0;

    /**
     * 删除标记
     * <p>标记该版本的数据是否已被删除</p>
     */
    @Builder.Default
    private boolean deleted = false;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof final ReleaseDateVersionMetaData that)) {
            return false;
        }
        return Objects.equals(getDataId(), that.getDataId())
                && Objects.equals(getDataType(), that.getDataType())
                && Objects.equals(getVersionDate(), that.getVersionDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataId(), getDataType(), getVersionDate());
    }
}
