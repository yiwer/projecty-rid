package com.github.ryan.facility.common;

import com.github.ryan.facility.structure.Tuple;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <b>常见数据结构工具类</b>
 * <p>
 * 提供集合、Map、字符串等常见数据结构的工具方法，
 * 包括空值判断、集合转换、深拷贝、业务等价比较等。
 * </p>
 * <p>
 * 所有方法均为 null 安全，不会抛出 NPE。
 * 返回集合的方法保证不返回 null（返回空集合）。
 * </p>
 *
 * @author yvvb
 * @since 2025/12/15
 */
@UtilityClass
public class CommonUtil {

    // ==================== Null Checks ====================

    /**
     * 判断对象是否为 null
     *
     * @param obj 对象
     *
     * @return true - 为 null
     */
    public static boolean isNull(@Nullable final Object obj) {
        return obj == null;
    }

    /**
     * 判断对象是否非 null
     *
     * @param obj 对象
     *
     * @return true - 非 null
     */
    public static boolean nonNull(@Nullable final Object obj) {
        return obj != null;
    }

    /**
     * 判断所有元素是否都非 null
     * <p>注意：如果数组本身为 null 或长度为 0，返回 false</p>
     *
     * @param elements 元素数组
     * @param <E>      元素类型
     *
     * @return true - 所有元素都非 null
     */
    @SafeVarargs
    public static <E> boolean allNotNull(E... elements) {
        if (elements == null || elements.length == 0) {
            return false;
        }
        for (E element : elements) {
            if (element == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * null 安全的 equals 比较
     *
     * @param a 对象 a
     * @param b 对象 b
     *
     * @return true - 相等
     */
    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        return Objects.equals(a, b);
    }

    // ==================== Empty Checks ====================

    /**
     * 判断集合是否为空
     *
     * @param collection 集合（可为 null）
     *
     * @return true - 为 null 或空集合
     */
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否非空
     *
     * @param collection 集合（可为 null）
     *
     * @return true - 非 null 且不为空
     */
    public static boolean isNotEmpty(@Nullable Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * 判断 Map 是否为空
     *
     * @param map Map（可为 null）
     *
     * @return true - 为 null 或空 Map
     */
    public static boolean isEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否非空
     *
     * @param map Map（可为 null）
     *
     * @return true - 非 null 且不为空
     */
    public static boolean isNotEmpty(@Nullable Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * 判断数组是否为空
     *
     * @param array 数组（可为 null）
     * @param <T>   元素类型
     *
     * @return true - 为 null 或长度为 0
     */
    public static <T> boolean isEmpty(@Nullable T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否非空
     *
     * @param array 数组（可为 null）
     * @param <T>   元素类型
     *
     * @return true - 非 null 且长度大于 0
     */
    public static <T> boolean isNotEmpty(@Nullable T[] array) {
        return array != null && array.length > 0;
    }

    /**
     * 判断字符序列是否为空白
     * <p>null、空字符串、纯空白字符都视为空白</p>
     *
     * @param cs 字符序列（可为 null）
     *
     * @return true - 为空白
     */
    public static boolean isBlank(@Nullable final CharSequence cs) {
        if (cs == null) {
            return true;
        }
        final int strLen = cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符序列是否非空白
     *
     * @param cs 字符序列（可为 null）
     *
     * @return true - 非空白
     */
    public static boolean isNotBlank(@Nullable final CharSequence cs) {
        return !isBlank(cs);
    }

    // ==================== Map Operations ====================

    /**
     * 集合转 Map（putIfAbsent 模式，保留先插入的值）
     * <p>
     * 跳过集合中的 null 元素。如果键提取函数返回 null，该元素也会被跳过。
     * </p>
     *
     * @param originCollection 原始集合（可为 null）
     * @param keyExtractor     键提取函数
     * @param <K>              键类型
     * @param <V>              值类型
     *
     * @return 转换后的 Map（永不为 null）
     */
    public static <K, V> Map<K, V> toMap(@Nullable Collection<V> originCollection, Function<V, K> keyExtractor) {
        if (isEmpty(originCollection)) {
            return new HashMap<>();
        }
        // 预估容量：size / 0.75 + 1
        Map<K, V> resultMap = new HashMap<>(calculateCapacity(originCollection.size()));
        for (V origin : originCollection) {
            if (origin != null) {
                K key = keyExtractor.apply(origin);
                if (key != null) {
                    resultMap.putIfAbsent(key, origin);
                }
            }
        }
        return resultMap;
    }

    /**
     * 集合转 Map（自定义键和值提取，过滤 null）
     * <p>
     * 跳过集合中的 null 元素。键或值为 null 的映射结果也会被跳过。
     * </p>
     *
     * @param originCollection 原始集合（可为 null）
     * @param keyExtractor     键提取函数
     * @param valueExtractor   值提取函数
     * @param <K>              键类型
     * @param <V>              值类型
     * @param <E>              集合元素类型
     *
     * @return 转换后的 Map（永不为 null）
     */
    public static <K, V, E> Map<K, V> toMap(@Nullable Collection<E> originCollection,
                                            Function<E, K> keyExtractor,
                                            Function<E, V> valueExtractor) {
        if (isEmpty(originCollection)) {
            return new HashMap<>();
        }
        Map<K, V> resultMap = new HashMap<>(calculateCapacity(originCollection.size()));
        for (E origin : originCollection) {
            if (origin != null) {
                final K key = keyExtractor.apply(origin);
                final V value = valueExtractor.apply(origin);
                if (key != null && value != null) {
                    resultMap.putIfAbsent(key, value);
                }
            }
        }
        return resultMap;
    }

    /**
     * 从 Map 中安全提取并转换值
     * <p>
     * 如果键存在且值类型匹配，返回包含值的 Optional；否则返回空 Optional。
     * </p>
     *
     * @param map  源 Map（可为 null）
     * @param key  键（可为 null）
     * @param type 目标类型
     * @param <K>  键类型
     * @param <E>  目标类型
     *
     * @return 转换后的 Optional 包装值
     */
    public static <K, E> Optional<E> safeExtractFromMap(@Nullable Map<K, ?> map, @Nullable K key, Class<E> type) {
        if (map == null || key == null || type == null) {
            return Optional.empty();
        }
        Object value = map.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * 提取两个 Map 的共有键并封装为比较元组
     * <p>
     * 只提取两个 Map 中都存在的键，封装为 Tuple(map1 的值, map2 的值)。
     * </p>
     *
     * @param map1 Map1（可为 null）
     * @param map2 Map2（可为 null）
     * @param <K>  键类型
     * @param <V>  值类型
     *
     * @return 比较元组 Map（永不为 null）
     */
    public static <K, V> Map<K, Tuple<V, V>> extractCompareTuple(@Nullable Map<K, V> map1, @Nullable Map<K, V> map2) {
        if (isEmpty(map1) || isEmpty(map2)) {
            return new HashMap<>();
        }
        // 结果集大小取决于较小的那个Map
        int expectedSize = Math.min(map1.size(), map2.size());
        Map<K, Tuple<V, V>> resultMap = new HashMap<>(calculateCapacity(expectedSize));

        map1.forEach((key, value) -> {
            if (map2.containsKey(key)) {
                resultMap.put(key, Tuple.of(value, map2.get(key)));
            }
        });
        return resultMap;
    }


    // ==================== List Operations ====================

    /**
     * 安全地聚合多个列表
     * <p>
     * 将多个列表按顺序合并为一个新列表。跳过 null 列表和空列表。
     * </p>
     *
     * @param lists 列表数组（可变参数）
     * @param <E>   元素类型
     *
     * @return 聚合后的列表（永不为 null）
     */
    @SafeVarargs
    public static <E> List<E> safelyJoin(List<E>... lists) {
        if (lists == null || lists.length == 0) {
            return new ArrayList<>();
        }
        int totalSize = 0;
        for (List<E> list : lists) {
            if (list != null) {
                totalSize += list.size();
            }
        }
        List<E> resultList = new ArrayList<>(totalSize);
        for (List<E> list : lists) {
            if (isNotEmpty(list)) {
                resultList.addAll(list);
            }
        }
        return resultList;
    }

    /**
     * 安全地转换并聚合多个列表
     * <p>
     * 将多个列表中的元素映射后合并为一个新列表。
     * 跳过 null 列表、空列表和 null 元素，映射结果为 null 也会被跳过。
     * </p>
     *
     * @param mapper 映射函数
     * @param lists  列表数组（可变参数）
     * @param <E>    原始元素类型
     * @param <R>    映射后的类型
     *
     * @return 聚合后的列表（永不为 null）
     */
    @SafeVarargs
    public static <E, R> List<R> safelyMappingAndJoin(Function<E, R> mapper, List<E>... lists) {
        if (lists == null || lists.length == 0) {
            return new ArrayList<>();
        }
        List<R> resultList = new ArrayList<>();
        for (List<E> list : lists) {
            if (isNotEmpty(list)) {
                for (E element : list) {
                    if (element != null) {
                        R mapped = mapper.apply(element);
                        if (mapped != null) {
                            resultList.add(mapped);
                        }
                    }
                }
            }
        }
        return resultList;
    }

    /**
     * 安全地映射列表元素（过滤 null）
     * <p>
     * 跳过源列表中的 null 元素和映射结果为 null 的元素。
     * 因此不保证结果列表与输入列表元素数量一致。
     * </p>
     *
     * @param originList 原始列表（可为 null）
     * @param mapper     映射函数
     * @param <E>        原始元素类型
     * @param <R>        映射后的类型
     *
     * @return 映射后的列表（永不为 null）
     */
    public static <E, R> List<R> mapNonNull(@Nullable List<E> originList, Function<E, R> mapper) {
        if (isEmpty(originList)) {
            return new ArrayList<>();
        }
        List<R> resultList = new ArrayList<>(originList.size());
        for (E e : originList) {
            if (e != null) {
                R mapped = mapper.apply(e);
                if (mapped != null) {
                    resultList.add(mapped);
                }
            }
        }
        return resultList;
    }

    /**
     * @deprecated 使用 {@link #mapNonNull} 代替
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static <E, R> List<R> filterNullMappingList(@Nullable List<E> originList, Function<E, R> mapper) {
        return mapNonNull(originList, mapper);
    }

    /**
     * 获取两个列表的差异元素
     * <p>
     * 返回在 list1 中存在但在 list2 中不存在（或数量不足）的元素。
     * 如果任一列表为 null，视为空列表处理。
     * </p>
     *
     * @param list1 列表 1（可为 null）
     * @param list2 列表 2（可为 null）
     * @param <T>   元素类型
     *
     * @return 差异元素列表（永不为 null）
     */
    public static <T> List<T> listDiff(@Nullable List<T> list1, @Nullable List<T> list2) {
        if (isEmpty(list1)) {
            return new ArrayList<>();
        }
        if (isEmpty(list2)) {
            return new ArrayList<>(list1);
        }
        // 优化：resultList 最大可能就是 list1 的大小
        List<T> resultList = new ArrayList<>(list1.size());

        // 计数 Map
        Map<T, Integer> countingMap = new HashMap<>(calculateCapacity(list1.size()));
        for (T t : list1) {
            countingMap.merge(t, 1, Integer::sum);
        }

        for (T t : list2) {
            countingMap.computeIfPresent(t, (k, count) -> count > 1 ? count - 1 : null);
            // 注意：原代码返回 0，这会导致 map key 还在但值为0，虽然遍历时 i<0 不会执行，但 map 不会变小。
            // 优化：如果减到0，直接返回 null，computeIfPresent 会移除该 Key，减少 Map 大小。
        }

        countingMap.forEach((k, count) -> {
            for (int i = 0; i < count; i++) {
                resultList.add(k);
            }
        });
        return resultList;
    }

    // ==================== Default Value Helpers ====================

    /**
     * 如果数据为 null 则返回默认值
     *
     * @param data         数据（可为 null）
     * @param defaultValue 默认值（可为 null）
     * @param <T>          数据类型
     *
     * @return 数据非 null 时返回数据，否则返回默认值
     */
    public static <T> T getOrDefault(@Nullable T data, @Nullable T defaultValue) {
        return data != null ? data : defaultValue;
    }

    /**
     * 计算并返回结果，如果结果为 null 则返回默认值
     *
     * @param dataSupplier 数据提供器（不可为 null）
     * @param defaultValue 默认值
     * @param <T>          数据类型
     *
     * @return 计算结果或默认值
     */
    public static <T> T computeOrElse(Supplier<T> dataSupplier, @Nullable T defaultValue) {
        Objects.requireNonNull(dataSupplier, "dataSupplier 不能为 null");
        T data = dataSupplier.get();
        return data != null ? data : defaultValue;
    }


    /**
     * 将可变参数数组转换为可修改的 ArrayList
     * <p>
     * 与 {@link Arrays#asList(Object[])} 不同，该方法返回的是一个真正的 ArrayList，
     * 支持 add、remove 等修改操作。
     * </p>
     *
     * @param arrays 可变参数数组（可为 null）
     * @param <T>    元素类型
     *
     * @return 包含所有元素的 ArrayList；如果输入为 null 或空数组，返回空的 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> asList(@Nullable T... arrays) {
        if (arrays == null || arrays.length == 0) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(arrays));
        }
    }

    // ==================== Business Logic ====================

    /**
     * 业务等价比较
     * <p>
     * 提供业务友好的等价性判断，包含以下特殊处理：
     * </p>
     * <ul>
     *     <li>null 和空字符串/空白字符串视为相等</li>
     *     <li>null 和数值 0 视为相等</li>
     *     <li>数值类型转换为 BigDecimal 比较（处理精度问题）</li>
     *     <li>null 和空数组视为相等（支持对象数组和原始类型数组）</li>
     *     <li>对 NaN/Infinity 有防御性处理</li>
     * </ul>
     *
     * @param value1 值 1（可为 null）
     * @param value2 值 2（可为 null）
     *
     * @return true - 业务等价
     */
    public static boolean businessEquals(@Nullable Object value1, @Nullable Object value2) {
        if (value1 == value2) {
            return true;
        }
        if (value1 == null || value2 == null) {
            // 处理 null vs ""
            if (value1 == null && value2 instanceof CharSequence cs) return isBlank(cs);
            if (value2 == null && value1 instanceof CharSequence cs) return isBlank(cs);
            // 处理 null vs 0
            if (value1 == null && value2 instanceof Number n) return isZero(n);
            if (value2 == null && value1 instanceof Number n) return isZero(n);
            // 处理 null vs 空数组 (Object[])
            if (value1 == null && value2 instanceof Object[] arr) return arr.length == 0;
            if (value2 == null && value1 instanceof Object[] arr) return arr.length == 0;
            // 处理 null vs 空数组 (Primitive Arrays) - 需要反射
            if (value1 == null && value2.getClass().isArray()) return Array.getLength(value2) == 0;
            if (value2 == null && value1.getClass().isArray()) return Array.getLength(value1) == 0;

            return false;
        }

        // 数值比较
        if (value1 instanceof Number n1 && value2 instanceof Number n2) {
            BigDecimal b1 = toBigDecimal(n1);
            BigDecimal b2 = toBigDecimal(n2);
            // 如果转换失败（返回null），退化为普通 equals
            if (b1 == null || b2 == null) {
                return value1.equals(value2);
            }
            return b1.compareTo(b2) == 0;
        }

        // 数组比较 (增强：支持原始类型数组)
        if (value1.getClass().isArray() && value2.getClass().isArray()) {
            // 如果是对象数组
            if (value1 instanceof Object[] arr1 && value2 instanceof Object[] arr2) {
                if (arr1.length == 0 && arr2.length == 0) return true;
                return Arrays.deepEquals(arr1, arr2); // 使用 deepEquals 更好，支持嵌套数组
            }
            // 原始数组比较比较麻烦，这里简单处理：长度为0视为相等，否则比较内容
            int len1 = Array.getLength(value1);
            int len2 = Array.getLength(value2);
            if (len1 == 0 && len2 == 0) return true;
            // 原始数组内容比较建议使用 Arrays 工具类的重载方法，或者转列表，
            // 鉴于通用性，这里可以比较长度后不做进一步深比较，或者退化为普通 equals (比较地址)
            // 这是一个权衡，为了完全准确可能需要 exhaustive check on array type.
            // 此处保持原意：利用 Objects.deepEquals
            return Objects.deepEquals(value1, value2);
        }

        return value1.equals(value2);
    }

    /**
     * 判断数值是否为零
     */
    private static boolean isZero(Number number) {
        if (number == null) {
            return true;
        }
        if (number instanceof BigDecimal bd) {
            return bd.compareTo(BigDecimal.ZERO) == 0;
        }
        if (number instanceof Double || number instanceof Float) {
            double d = number.doubleValue();
            // NaN 和 Infinity 不视为 0
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return false;
            }
            return d == 0.0;
        }
        return number.longValue() == 0;
    }

    /**
     * 将 Number 转换为 BigDecimal（处理精度问题）
     * <p>
     * 对于整数类型使用 valueOf(long)，对于浮点类型使用字符串构造避免精度损失。
     * </p>
     */
    @Nullable
    private static BigDecimal toBigDecimal(Number number) {
        if (number == null) {
            return null;
        }
        try {
            if (number instanceof BigDecimal bd) {
                return bd;
            }
            if (number instanceof Long || number instanceof Integer
                    || number instanceof Short || number instanceof Byte) {
                return BigDecimal.valueOf(number.longValue());
            }
            // Double/Float - 使用字符串构造避免精度问题
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return null;
            }
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 计算 HashMap 初始容量，避免扩容
     *
     * @param expectedSize 预期元素数量
     *
     * @return 初始容量
     */
    public static int calculateCapacity(int expectedSize) {
        if (expectedSize <= 0) {
            return 16;
        }
        // 基于负载因子 0.75 计算：expectedSize / 0.75 + 1
        return (int) Math.ceil(expectedSize / 0.75) + 1;
    }
}