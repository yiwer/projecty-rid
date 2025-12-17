package com.github.ryan.version.release_date;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <b>数据版本类型枚举</b>
 * <p>
 * 定义数据版本的变更类型，包括无变化、新增、修改、删除。
 * 同时定义了版本类型之间的合理转换规则。
 * </p>
 *
 * <h3>版本类型转换规则：</h3>
 * <ul>
 *     <li>NO_CHANGE → MODIFY, DELETE</li>
 *     <li>CREATE → DELETE</li>
 *     <li>MODIFY → MODIFY, DELETE</li>
 *     <li>DELETE → DELETE</li>
 * </ul>
 *
 * @author yvvb
 * @since 2025/5/5
 */
@Getter
public enum ReleaseVersionType {

    /**
     * 无变化，表示版本数据未发生修改
     */
    NO_CHANGE("NoChange"),

    /**
     * 新增，表示创建了新的数据版本
     */
    CREATE("Create"),

    /**
     * 修改，表示对现有版本进行了修改
     */
    MODIFY("Modify"),

    /**
     * 删除，表示数据被删除
     */
    DELETE("Delete"),
    ;
    /**
     * 版本类型转换规则映射
     * <p>
     * 定义从某个版本类型可以合理转换到哪些版本类型
     * </p>
     */
    public static final Map<ReleaseVersionType, Set<ReleaseVersionType>> RATIONAL_TRANSFER_RULES = new HashMap<>();
    private static final Map<String, ReleaseVersionType> CODE_MAP = new HashMap<>();

    static {
        for (final ReleaseVersionType value : ReleaseVersionType.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    static {
        RATIONAL_TRANSFER_RULES.put(NO_CHANGE, Set.of(MODIFY, DELETE));
        RATIONAL_TRANSFER_RULES.put(CREATE, Set.of(DELETE));
        RATIONAL_TRANSFER_RULES.put(MODIFY, Set.of(NO_CHANGE, MODIFY, DELETE));
        RATIONAL_TRANSFER_RULES.put(DELETE, Set.of(DELETE));
    }

    /**
     * 版本类型编码
     */
    private final String code;

    /**
     * 构造函数
     *
     * @param code 版本类型编码
     * @param name 版本类型名称
     */
    ReleaseVersionType(String code) {
        this.code = code;
    }

    /**
     * 检查版本类型转换是否合理
     * <p>
     * 根据预定义的转换规则判断从 from 到 to 的转换是否允许
     * </p>
     *
     * @param from 源版本类型
     * @param to   目标版本类型
     *
     * @return true-转换合理，false-转换不合理
     */
    public static boolean checkTransferValid(ReleaseVersionType from, ReleaseVersionType to) {
        if (from == null || to == null) {
            return false;
        }
        final var allowTransitionSet = RATIONAL_TRANSFER_RULES.get(from);
        return allowTransitionSet != null && allowTransitionSet.contains(to);
    }

    /**
     * 根据编码获取版本类型
     * <p>
     * 使用缓存数组提高性能
     * </p>
     *
     * @param code 版本类型编码
     *
     * @return 对应的版本类型，编码无效时返回null
     */
    public static Optional<ReleaseVersionType> fromCode(String code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }
}
