package com.github.ryan.facility.copy;

import com.github.ryan.facility.common.CommonUtil;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class CopyUtil {

    // ==================== Deep Copy ====================

    /**
     * 列表深拷贝
     * <p>
     * 对列表中的每个元素进行深拷贝，要求元素类型实现 {@link CopyTrait} 接口。
     * 会跳过列表中的 null 元素。
     * </p>
     *
     * @param originList 原始列表（可为 null）
     * @param <E>        元素类型，必须实现 CopyTrait
     * @return 深拷贝后的新列表（永不为 null）
     */
    public static <E extends CopyTrait<E>> List<E> copyList(@Nullable List<E> originList) {
        if (CommonUtil.isEmpty(originList)) {
            return new ArrayList<>();
        }
        List<E> resultList = new ArrayList<>(originList.size());
        for (E origin : originList) {
            if (origin != null) {
                resultList.add(origin.copy());
            }
        }
        return resultList;
    }

    /**
     * Set 深拷贝
     * <p>
     * 对 Set 中的每个元素进行深拷贝，要求元素类型实现 {@link CopyTrait} 接口。
     * 会跳过 Set 中的 null 元素。
     * </p>
     *
     * @param originSet 原始 Set（可为 null）
     * @param <E>       元素类型，必须实现 CopyTrait
     * @return 深拷贝后的新 Set（永不为 null）
     */
    public static <E extends CopyTrait<E>> Set<E> copySet(@Nullable Set<E> originSet) {
        if (CommonUtil.isEmpty(originSet)) {
            return new HashSet<>();
        }
        Set<E> resultSet = new HashSet<>(CommonUtil.calculateCapacity(originSet.size()));
        for (E origin : originSet) {
            if (origin != null) {
                resultSet.add(origin.copy());
            }
        }
        return resultSet;
    }

    /**
     * Map 深拷贝 Only Values
     * <p>
     * 对 Map 中的每个值进行深拷贝，要求值类型实现 {@link CopyTrait} 接口。
     * 会跳过值为 null 的条目。
     * </p>
     *
     * @param originMap 原始 Map（可为 null）
     * @param <K>       键类型 ,不会copy Key，请保证key是基本类型
     * @param <V>       值类型，必须实现 CopyTrait
     * @return 深拷贝后的新 Map（永不为 null）
     */
    public static <K, V extends CopyTrait<V>> Map<K, V> copyMapValues(@Nullable Map<K, V> originMap) {
        if (CommonUtil.isEmpty(originMap)) {
            return new HashMap<>();
        }
        Map<K, V> resultMap = new HashMap<>(CommonUtil.calculateCapacity(originMap.size()));
        originMap.forEach((key, value) -> {
            if (value != null) {
                resultMap.put(key, value.copy());
            }
        });
        return resultMap;
    }

    /**
     * Map 深拷贝 ALL
     * <p>
     * 对 Map 中的每个键值进行深拷贝，要求键值类型都实现 {@link CopyTrait} 接口。
     * 会跳过值为 null 的条目。
     * </p>
     *
     * @param originMap 原始 Map（可为 null）
     * @param <K>       键类型, 必须实现 CopyTrait
     * @param <V>       值类型，必须实现 CopyTrait
     * @return 深拷贝后的新 Map（永不为 null）
     */
    public static <K extends CopyTrait<K>, V extends CopyTrait<V>> Map<K, V> copyMapAll(@Nullable Map<K, V> originMap) {
        if (CommonUtil.isEmpty(originMap)) {
            return new HashMap<>();
        }
        Map<K, V> resultMap = new HashMap<>(CommonUtil.calculateCapacity(originMap.size()));
        originMap.forEach((key, value) -> {
            if (value != null) {
                resultMap.put(key.copy(), value.copy());
            }
        });
        return resultMap;
    }
}
