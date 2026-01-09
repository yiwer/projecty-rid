package com.github.ryan.facility.common;

import com.github.ryan.facility.structure.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CommonUtil 工具类测试")
class CommonUtilTest {

    @Nested
    @DisplayName("Null 检查")
    class NullChecks {

        @Test
        @DisplayName("isNull - 对象为 null")
        void isNull_whenObjectIsNull_returnsTrue() {
            assertThat(CommonUtil.isNull(null)).isTrue();
        }

        @Test
        @DisplayName("isNull - 对象非 null")
        void isNull_whenObjectIsNotNull_returnsFalse() {
            assertThat(CommonUtil.isNull("test")).isFalse();
        }

        @Test
        @DisplayName("nonNull - 对象非 null")
        void nonNull_whenObjectIsNotNull_returnsTrue() {
            assertThat(CommonUtil.nonNull("test")).isTrue();
        }

        @Test
        @DisplayName("nonNull - 对象为 null")
        void nonNull_whenObjectIsNull_returnsFalse() {
            assertThat(CommonUtil.nonNull(null)).isFalse();
        }

        @Test
        @DisplayName("allNotNull - 所有元素非 null")
        void allNotNull_whenAllElementsAreNotNull_returnsTrue() {
            assertThat(CommonUtil.allNotNull("a", "b", "c")).isTrue();
        }

        @Test
        @DisplayName("allNotNull - 存在 null 元素")
        void allNotNull_whenAnyElementIsNull_returnsFalse() {
            assertThat(CommonUtil.allNotNull("a", null, "c")).isFalse();
        }

        @Test
        @DisplayName("allNotNull - 数组为 null")
        void allNotNull_whenArrayIsNull_returnsFalse() {
            assertThat(CommonUtil.allNotNull((String[]) null)).isFalse();
        }

        @Test
        @DisplayName("allNotNull - 数组为空")
        void allNotNull_whenArrayIsEmpty_returnsFalse() {
            assertThat(CommonUtil.allNotNull()).isFalse();
        }

        @Test
        @DisplayName("equals - 两个 null")
        void equals_whenBothNull_returnsTrue() {
            assertThat(CommonUtil.equals(null, null)).isTrue();
        }

        @Test
        @DisplayName("equals - 一个 null")
        void equals_whenOneNull_returnsFalse() {
            assertThat(CommonUtil.equals("a", null)).isFalse();
            assertThat(CommonUtil.equals(null, "a")).isFalse();
        }

        @Test
        @DisplayName("equals - 相等对象")
        void equals_whenEqual_returnsTrue() {
            assertThat(CommonUtil.equals("test", "test")).isTrue();
        }

        @Test
        @DisplayName("equals - 不相等对象")
        void equals_whenNotEqual_returnsFalse() {
            assertThat(CommonUtil.equals("a", "b")).isFalse();
        }
    }

    @Nested
    @DisplayName("Empty 检查")
    class EmptyChecks {

        @Test
        @DisplayName("isEmpty(Collection) - null 集合")
        void isEmpty_whenCollectionIsNull_returnsTrue() {
            assertThat(CommonUtil.isEmpty((Collection<?>) null)).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Collection) - 空集合")
        void isEmpty_whenCollectionIsEmpty_returnsTrue() {
            assertThat(CommonUtil.isEmpty(Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Collection) - 非空集合")
        void isEmpty_whenCollectionIsNotEmpty_returnsFalse() {
            assertThat(CommonUtil.isEmpty(List.of("a"))).isFalse();
        }

        @Test
        @DisplayName("isNotEmpty(Collection) - 非空集合")
        void isNotEmpty_whenCollectionIsNotEmpty_returnsTrue() {
            assertThat(CommonUtil.isNotEmpty(List.of("a"))).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Map) - null Map")
        void isEmpty_whenMapIsNull_returnsTrue() {
            assertThat(CommonUtil.isEmpty((Map<?, ?>) null)).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Map) - 空 Map")
        void isEmpty_whenMapIsEmpty_returnsTrue() {
            assertThat(CommonUtil.isEmpty(Collections.emptyMap())).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Map) - 非空 Map")
        void isEmpty_whenMapIsNotEmpty_returnsFalse() {
            assertThat(CommonUtil.isEmpty(Map.of("key", "value"))).isFalse();
        }

        @Test
        @DisplayName("isEmpty(Array) - null 数组")
        void isEmpty_whenArrayIsNull_returnsTrue() {
            assertThat(CommonUtil.isEmpty((String[]) null)).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Array) - 空数组")
        void isEmpty_whenArrayIsEmpty_returnsTrue() {
            assertThat(CommonUtil.isEmpty(new String[0])).isTrue();
        }

        @Test
        @DisplayName("isEmpty(Array) - 非空数组")
        void isEmpty_whenArrayIsNotEmpty_returnsFalse() {
            assertThat(CommonUtil.isEmpty(new String[]{"a"})).isFalse();
        }

        @Test
        @DisplayName("isBlank - null 字符串")
        void isBlank_whenNull_returnsTrue() {
            assertThat(CommonUtil.isBlank(null)).isTrue();
        }

        @Test
        @DisplayName("isBlank - 空字符串")
        void isBlank_whenEmpty_returnsTrue() {
            assertThat(CommonUtil.isBlank("")).isTrue();
        }

        @Test
        @DisplayName("isBlank - 空白字符串")
        void isBlank_whenWhitespace_returnsTrue() {
            assertThat(CommonUtil.isBlank("   ")).isTrue();
            assertThat(CommonUtil.isBlank("\t\n")).isTrue();
        }

        @Test
        @DisplayName("isBlank - 非空白字符串")
        void isBlank_whenNotBlank_returnsFalse() {
            assertThat(CommonUtil.isBlank("test")).isFalse();
            assertThat(CommonUtil.isBlank(" a ")).isFalse();
        }

        @Test
        @DisplayName("isNotBlank - 非空白字符串")
        void isNotBlank_whenNotBlank_returnsTrue() {
            assertThat(CommonUtil.isNotBlank("test")).isTrue();
        }
    }

    @Nested
    @DisplayName("Map 操作")
    class MapOperations {

        @Test
        @DisplayName("toMap - 集合转 Map（键提取）")
        void toMap_whenValidCollection_returnsMap() {
            List<String> list = List.of("apple", "banana", "cherry");
            Map<Integer, String> result = CommonUtil.toMap(list, String::length);
            
            assertThat(result).hasSize(2);
            assertThat(result.get(5)).isEqualTo("apple");
            assertThat(result.get(6)).isIn("banana", "cherry");
        }

        @Test
        @DisplayName("toMap - null 集合")
        void toMap_whenNullCollection_returnsEmptyMap() {
            Map<Integer, String> result = CommonUtil.toMap(null, String::length);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("toMap - 跳过 null 元素")
        void toMap_whenCollectionHasNullElements_skipsNulls() {
            List<String> list = Arrays.asList("a", null, "b");
            Map<Integer, String> result = CommonUtil.toMap(list, String::length);
            
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("toMap - 键值提取")
        void toMap_whenExtractingKeyAndValue_returnsMap() {
            List<String> list = List.of("apple", "banana");
            Map<String, Integer> result = CommonUtil.toMap(
                list,
                s -> s.substring(0, 1),
                String::length
            );
            
            assertThat(result).hasSize(2);
            assertThat(result.get("a")).isEqualTo(5);
            assertThat(result.get("b")).isEqualTo(6);
        }

        @Test
        @DisplayName("safeExtractFromMap - 成功提取")
        void safeExtractFromMap_whenKeyExistsAndTypeMatches_returnsValue() {
            Map<String, Object> map = Map.of("key", "value");
            Optional<String> result = CommonUtil.safeExtractFromMap(map, "key", String.class);
            
            assertThat(result).isPresent().contains("value");
        }

        @Test
        @DisplayName("safeExtractFromMap - 类型不匹配")
        void safeExtractFromMap_whenTypeMismatch_returnsEmpty() {
            Map<String, Object> map = Map.of("key", 123);
            Optional<String> result = CommonUtil.safeExtractFromMap(map, "key", String.class);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("safeExtractFromMap - 键不存在")
        void safeExtractFromMap_whenKeyNotExists_returnsEmpty() {
            Map<String, Object> map = Map.of("key", "value");
            Optional<String> result = CommonUtil.safeExtractFromMap(map, "other", String.class);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("safeExtractFromMap - null Map")
        void safeExtractFromMap_whenNullMap_returnsEmpty() {
            Optional<String> result = CommonUtil.safeExtractFromMap(null, "key", String.class);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("extractCompareTuple - 提取共有键")
        void extractCompareTuple_whenCommonKeys_returnsCompareTuples() {
            Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
            Map<String, Integer> map2 = Map.of("b", 20, "c", 30, "d", 40);
            
            Map<String, Tuple<Integer, Integer>> result = CommonUtil.extractCompareTuple(map1, map2);
            
            assertThat(result).hasSize(2);
            assertThat(result.get("b")).isEqualTo(Tuple.of(2, 20));
            assertThat(result.get("c")).isEqualTo(Tuple.of(3, 30));
        }

        @Test
        @DisplayName("extractCompareTuple - 无共有键")
        void extractCompareTuple_whenNoCommonKeys_returnsEmptyMap() {
            Map<String, Integer> map1 = Map.of("a", 1);
            Map<String, Integer> map2 = Map.of("b", 2);
            
            Map<String, Tuple<Integer, Integer>> result = CommonUtil.extractCompareTuple(map1, map2);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("extractCompareTuple - null Map")
        void extractCompareTuple_whenNullMap_returnsEmptyMap() {
            Map<String, Integer> map = Map.of("a", 1);
            
            assertThat(CommonUtil.extractCompareTuple(null, map)).isEmpty();
            assertThat(CommonUtil.extractCompareTuple(map, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("List 操作")
    class ListOperations {

        @Test
        @DisplayName("safelyJoin - 聚合多个列表")
        void safelyJoin_whenMultipleLists_joinsThem() {
            List<String> list1 = List.of("a", "b");
            List<String> list2 = List.of("c", "d");
            List<String> list3 = List.of("e");
            
            List<String> result = CommonUtil.safelyJoin(list1, list2, list3);
            
            assertThat(result).containsExactly("a", "b", "c", "d", "e");
        }

        @Test
        @DisplayName("safelyJoin - 跳过 null 列表")
        void safelyJoin_whenNullLists_skipsNulls() {
            List<String> list1 = List.of("a");
            List<String> list2 = null;
            List<String> list3 = List.of("b");
            
            List<String> result = CommonUtil.safelyJoin(list1, list2, list3);
            
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        @DisplayName("safelyJoin - 空列表数组")
        void safelyJoin_whenNoLists_returnsEmptyList() {
            List<String> result = CommonUtil.safelyJoin();
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("safelyMappingAndJoin - 映射并聚合")
        void safelyMappingAndJoin_whenMapping_mapsAndJoins() {
            List<String> list1 = List.of("a", "bb");
            List<String> list2 = List.of("ccc");
            
            List<Integer> result = CommonUtil.safelyMappingAndJoin(String::length, list1, list2);
            
            assertThat(result).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("safelyMappingAndJoin - 跳过 null 元素")
        void safelyMappingAndJoin_whenNullElements_skipsNulls() {
            List<String> list = Arrays.asList("a", null, "b");
            
            List<Integer> result = CommonUtil.safelyMappingAndJoin(String::length, list);
            
            assertThat(result).containsExactly(1, 1);
        }

        @Test
        @DisplayName("mapNonNull - 过滤 null")
        void mapNonNull_whenMapping_skipsNullResults() {
            List<String> list = Arrays.asList("1", "not-a-number", "2", null);
            
            List<Integer> result = CommonUtil.mapNonNull(list, s -> {
                try {
                    return Integer.parseInt(s);
                } catch (Exception e) {
                    return null;
                }
            });
            
            assertThat(result).containsExactly(1, 2);
        }

        @Test
        @DisplayName("mapNonNull - null 列表")
        void mapNonNull_whenNullList_returnsEmptyList() {
            List<Integer> result = CommonUtil.mapNonNull(null, String::length);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("listDiff - 列表差异")
        void listDiff_whenLists_returnsDifference() {
            List<String> list1 = List.of("a", "b", "c", "b");
            List<String> list2 = List.of("b", "c", "d");
            
            List<String> result = CommonUtil.listDiff(list1, list2);
            
            assertThat(result).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("listDiff - list2 为空")
        void listDiff_whenList2IsEmpty_returnsAllOfList1() {
            List<String> list1 = List.of("a", "b");
            
            List<String> result = CommonUtil.listDiff(list1, Collections.emptyList());
            
            assertThat(result).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("listDiff - list1 为空")
        void listDiff_whenList1IsEmpty_returnsEmptyList() {
            List<String> result = CommonUtil.listDiff(Collections.emptyList(), List.of("a"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("asList - 可变参数转列表")
        void asList_whenVarargs_returnsModifiableList() {
            List<String> result = CommonUtil.asList("a", "b", "c");
            
            assertThat(result).containsExactly("a", "b", "c");
            assertThatCode(() -> result.add("d")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("asList - null 参数")
        void asList_whenNull_returnsEmptyList() {
            List<String> result = CommonUtil.asList((String[]) null);
            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("默认值辅助")
    class DefaultValueHelpers {

        @Test
        @DisplayName("getOrDefault - 值非 null")
        void getOrDefault_whenValueIsNotNull_returnsValue() {
            String result = CommonUtil.getOrDefault("value", "default");
            assertThat(result).isEqualTo("value");
        }

        @Test
        @DisplayName("getOrDefault - 值为 null")
        void getOrDefault_whenValueIsNull_returnsDefault() {
            String result = CommonUtil.getOrDefault(null, "default");
            assertThat(result).isEqualTo("default");
        }

        @Test
        @DisplayName("computeOrElse - 计算结果非 null")
        void computeOrElse_whenComputedValueIsNotNull_returnsComputed() {
            String result = CommonUtil.computeOrElse(() -> "computed", "default");
            assertThat(result).isEqualTo("computed");
        }

        @Test
        @DisplayName("computeOrElse - 计算结果为 null")
        void computeOrElse_whenComputedValueIsNull_returnsDefault() {
            String result = CommonUtil.computeOrElse(() -> null, "default");
            assertThat(result).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("业务逻辑")
    class BusinessLogic {

        @Test
        @DisplayName("businessEquals - null vs 空字符串")
        void businessEquals_whenNullAndEmptyString_returnsTrue() {
            assertThat(CommonUtil.businessEquals(null, "")).isTrue();
            assertThat(CommonUtil.businessEquals("", null)).isTrue();
            assertThat(CommonUtil.businessEquals(null, "   ")).isTrue();
        }

        @Test
        @DisplayName("businessEquals - null vs 0")
        void businessEquals_whenNullAndZero_returnsTrue() {
            assertThat(CommonUtil.businessEquals(null, 0)).isTrue();
            assertThat(CommonUtil.businessEquals(0, null)).isTrue();
            assertThat(CommonUtil.businessEquals(null, 0L)).isTrue();
            assertThat(CommonUtil.businessEquals(null, 0.0)).isTrue();
            assertThat(CommonUtil.businessEquals(null, BigDecimal.ZERO)).isTrue();
        }

        @Test
        @DisplayName("businessEquals - 数值比较")
        void businessEquals_whenNumbers_comparesAsDecimal() {
            assertThat(CommonUtil.businessEquals(1, 1L)).isTrue();
            assertThat(CommonUtil.businessEquals(1.0, 1)).isTrue();
            assertThat(CommonUtil.businessEquals(new BigDecimal("1.00"), 1)).isTrue();
            assertThat(CommonUtil.businessEquals(1.5, 2.5)).isFalse();
        }

        @Test
        @DisplayName("businessEquals - null vs 空数组")
        void businessEquals_whenNullAndEmptyArray_returnsTrue() {
            assertThat(CommonUtil.businessEquals(null, new String[0])).isTrue();
            assertThat(CommonUtil.businessEquals(new Object[0], null)).isTrue();
            assertThat(CommonUtil.businessEquals(null, new int[0])).isTrue();
        }

        @Test
        @DisplayName("businessEquals - 数组比较")
        void businessEquals_whenArrays_comparesContent() {
            assertThat(CommonUtil.businessEquals(new String[]{"a", "b"}, new String[]{"a", "b"})).isTrue();
            assertThat(CommonUtil.businessEquals(new int[]{1, 2}, new int[]{1, 2})).isTrue();
            assertThat(CommonUtil.businessEquals(new String[]{"a"}, new String[]{"b"})).isFalse();
        }

        @Test
        @DisplayName("businessEquals - 相同对象")
        void businessEquals_whenSameObject_returnsTrue() {
            String obj = "test";
            assertThat(CommonUtil.businessEquals(obj, obj)).isTrue();
        }

        @Test
        @DisplayName("calculateCapacity - 计算 HashMap 容量")
        void calculateCapacity_whenPositiveSize_calculatesCorrectly() {
            assertThat(CommonUtil.calculateCapacity(10)).isEqualTo(15);
            assertThat(CommonUtil.calculateCapacity(100)).isEqualTo(135);
        }

        @Test
        @DisplayName("calculateCapacity - 零或负数")
        void calculateCapacity_whenZeroOrNegative_returns16() {
            assertThat(CommonUtil.calculateCapacity(0)).isEqualTo(16);
            assertThat(CommonUtil.calculateCapacity(-5)).isEqualTo(16);
        }
    }
}
