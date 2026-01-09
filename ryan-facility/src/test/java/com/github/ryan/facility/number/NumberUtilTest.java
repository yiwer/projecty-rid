package com.github.ryan.facility.number;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NumberUtil 工具类测试")
class NumberUtilTest {

    @Nested
    @DisplayName("格式化方法")
    class FormatMethods {

        @Test
        @DisplayName("format - 格式化为指定小数位")
        void format_whenValidValue_formatsCorrectly() {
            assertThat(NumberUtil.format(new BigDecimal("123.456"), 2))
                .isEqualTo("123.46");
            assertThat(NumberUtil.format(new BigDecimal("123.456"), 0))
                .isEqualTo("123");
            assertThat(NumberUtil.format(new BigDecimal("123.456"), 4))
                .isEqualTo("123.4560");
        }

        @Test
        @DisplayName("format - null 值返回空字符串")
        void format_whenNull_returnsEmptyString() {
            assertThat(NumberUtil.format(null, 2)).isEmpty();
        }

        @Test
        @DisplayName("formatInt - 格式化为整数")
        void formatInt_whenValidValue_formatsAsInteger() {
            assertThat(NumberUtil.formatInt(123.456)).isEqualTo("123");
            assertThat(NumberUtil.formatInt(123)).isEqualTo("123");
        }

        @Test
        @DisplayName("formatInt - null 值返回空字符串")
        void formatInt_whenNull_returnsEmptyString() {
            assertThat(NumberUtil.formatInt(null)).isEmpty();
        }

        @Test
        @DisplayName("format2 - 格式化为2位小数")
        void format2_whenValidValue_formats2Decimals() {
            assertThat(NumberUtil.format2(new BigDecimal("123.456")))
                .isEqualTo("123.46");
        }

        @Test
        @DisplayName("format4 - 格式化为4位小数")
        void format4_whenValidValue_formats4Decimals() {
            assertThat(NumberUtil.format4(new BigDecimal("123.456")))
                .isEqualTo("123.4560");
        }

        @Test
        @DisplayName("formatSmart - 智能格式化去除尾部零")
        void formatSmart_whenValidValue_removesTrailingZeros() {
            assertThat(NumberUtil.formatSmart(new BigDecimal("123.0")))
                .isEqualTo("123");
            assertThat(NumberUtil.formatSmart(new BigDecimal("123.4560")))
                .isEqualTo("123.456");
            assertThat(NumberUtil.formatSmart(new BigDecimal("123.456789123")))
                .isEqualTo("123.45678912");
        }

        @Test
        @DisplayName("formatSmart - null 值返回空字符串")
        void formatSmart_whenNull_returnsEmptyString() {
            assertThat(NumberUtil.formatSmart(null)).isEmpty();
        }

        @Test
        @DisplayName("formatMoney - 格式化为金额（千分位）")
        void formatMoney_whenValidValue_formatsWithThousandsSeparator() {
            assertThat(NumberUtil.formatMoney(new BigDecimal("1234567.89")))
                .isEqualTo("1,234,567.89");
            assertThat(NumberUtil.formatMoney(new BigDecimal("100.00")))
                .isEqualTo("100.00");
        }

        @Test
        @DisplayName("formatMoney - null 值返回空字符串")
        void formatMoney_whenNull_returnsEmptyString() {
            assertThat(NumberUtil.formatMoney(null)).isEmpty();
        }

        @Test
        @DisplayName("formatPercent - 格式化为百分比")
        void formatPercent_whenValidValue_formatsAsPercent() {
            assertThat(NumberUtil.formatPercent(new BigDecimal("0.1234"), 2))
                .isEqualTo("12.34%");
            assertThat(NumberUtil.formatPercent(new BigDecimal("0.5"), 0))
                .isEqualTo("50%");
        }

        @Test
        @DisplayName("formatPercent - null 值返回空字符串")
        void formatPercent_whenNull_returnsEmptyString() {
            assertThat(NumberUtil.formatPercent(null, 2)).isEmpty();
        }
    }

    @Nested
    @DisplayName("安全解析方法")
    class SafeParseMethods {

        @Test
        @DisplayName("parseBigDecimal - 成功解析")
        void parseBigDecimal_whenValidString_parsesSuccessfully() {
            Optional<BigDecimal> result = NumberUtil.parseBigDecimal("123.45");
            
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo("123.45");
        }

        @Test
        @DisplayName("parseBigDecimal - 解析失败返回 empty")
        void parseBigDecimal_whenInvalidString_returnsEmpty() {
            assertThat(NumberUtil.parseBigDecimal("not-a-number")).isEmpty();
            assertThat(NumberUtil.parseBigDecimal(null)).isEmpty();
            assertThat(NumberUtil.parseBigDecimal("")).isEmpty();
            assertThat(NumberUtil.parseBigDecimal("  ")).isEmpty();
        }

        @Test
        @DisplayName("parseBigDecimalOrDefault - 解析成功返回值")
        void parseBigDecimalOrDefault_whenValidString_returnsValue() {
            BigDecimal result = NumberUtil.parseBigDecimalOrDefault("123.45", BigDecimal.ZERO);
            assertThat(result).isEqualByComparingTo("123.45");
        }

        @Test
        @DisplayName("parseBigDecimalOrDefault - 解析失败返回默认值")
        void parseBigDecimalOrDefault_whenInvalidString_returnsDefault() {
            BigDecimal result = NumberUtil.parseBigDecimalOrDefault("invalid", BigDecimal.TEN);
            assertThat(result).isEqualByComparingTo(BigDecimal.TEN);
        }

        @Test
        @DisplayName("parseBigDecimalOrZero - 解析失败返回 ZERO")
        void parseBigDecimalOrZero_whenInvalidString_returnsZero() {
            BigDecimal result = NumberUtil.parseBigDecimalOrZero("invalid");
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("parseInt - 成功解析")
        void parseInt_whenValidString_parsesSuccessfully() {
            Optional<Integer> result = NumberUtil.parseInt("123");
            
            assertThat(result).isPresent().hasValue(123);
        }

        @Test
        @DisplayName("parseInt - 解析失败返回 empty")
        void parseInt_whenInvalidString_returnsEmpty() {
            assertThat(NumberUtil.parseInt("not-a-number")).isEmpty();
            assertThat(NumberUtil.parseInt("123.45")).isEmpty();
            assertThat(NumberUtil.parseInt(null)).isEmpty();
        }

        @Test
        @DisplayName("parseIntOrDefault - 解析失败返回默认值")
        void parseIntOrDefault_whenInvalidString_returnsDefault() {
            int result = NumberUtil.parseIntOrDefault("invalid", 100);
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("parseLong - 成功解析")
        void parseLong_whenValidString_parsesSuccessfully() {
            Optional<Long> result = NumberUtil.parseLong("123456789");
            
            assertThat(result).isPresent().hasValue(123456789L);
        }

        @Test
        @DisplayName("parseLong - 解析失败返回 empty")
        void parseLong_whenInvalidString_returnsEmpty() {
            assertThat(NumberUtil.parseLong("invalid")).isEmpty();
            assertThat(NumberUtil.parseLong(null)).isEmpty();
        }

        @Test
        @DisplayName("parseLongOrDefault - 解析失败返回默认值")
        void parseLongOrDefault_whenInvalidString_returnsDefault() {
            long result = NumberUtil.parseLongOrDefault("invalid", 999L);
            assertThat(result).isEqualTo(999L);
        }

        @Test
        @DisplayName("parseDouble - 成功解析")
        void parseDouble_whenValidString_parsesSuccessfully() {
            Optional<Double> result = NumberUtil.parseDouble("123.45");
            
            assertThat(result).isPresent().hasValue(123.45);
        }

        @Test
        @DisplayName("parseDouble - 解析失败返回 empty")
        void parseDouble_whenInvalidString_returnsEmpty() {
            assertThat(NumberUtil.parseDouble("invalid")).isEmpty();
            assertThat(NumberUtil.parseDouble(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("比较方法")
    class ComparisonMethods {

        @Test
        @DisplayName("equals - BigDecimal 比较")
        void equals_whenBigDecimals_comparesCorrectly() {
            assertThat(NumberUtil.equals(
                new BigDecimal("1.0"),
                new BigDecimal("1.00")
            )).isTrue();
            
            assertThat(NumberUtil.equals(
                new BigDecimal("1.0"),
                new BigDecimal("2.0")
            )).isFalse();
        }

        @Test
        @DisplayName("equals - null 处理")
        void equals_whenNull_handlesCorrectly() {
            assertThat(NumberUtil.equals(null, null)).isTrue();
            assertThat(NumberUtil.equals(null, BigDecimal.ONE)).isFalse();
            assertThat(NumberUtil.equals(BigDecimal.ONE, null)).isFalse();
        }

        @Test
        @DisplayName("equals - 相同引用")
        void equals_whenSameReference_returnsTrue() {
            BigDecimal value = new BigDecimal("123.45");
            assertThat(NumberUtil.equals(value, value)).isTrue();
        }

        @Test
        @DisplayName("isPositive - 正数判断")
        void isPositive_whenPositiveNumber_returnsTrue() {
            assertThat(NumberUtil.isPositive(new BigDecimal("1"))).isTrue();
            assertThat(NumberUtil.isPositive(new BigDecimal("0.01"))).isTrue();
        }

        @Test
        @DisplayName("isPositive - 零和负数")
        void isPositive_whenZeroOrNegative_returnsFalse() {
            assertThat(NumberUtil.isPositive(BigDecimal.ZERO)).isFalse();
            assertThat(NumberUtil.isPositive(new BigDecimal("-1"))).isFalse();
            assertThat(NumberUtil.isPositive(null)).isFalse();
        }

        @Test
        @DisplayName("isNegative - 负数判断")
        void isNegative_whenNegativeNumber_returnsTrue() {
            assertThat(NumberUtil.isNegative(new BigDecimal("-1"))).isTrue();
            assertThat(NumberUtil.isNegative(new BigDecimal("-0.01"))).isTrue();
        }

        @Test
        @DisplayName("isNegative - 零和正数")
        void isNegative_whenZeroOrPositive_returnsFalse() {
            assertThat(NumberUtil.isNegative(BigDecimal.ZERO)).isFalse();
            assertThat(NumberUtil.isNegative(new BigDecimal("1"))).isFalse();
            assertThat(NumberUtil.isNegative(null)).isFalse();
        }

        @Test
        @DisplayName("isZero - 零判断")
        void isZero_whenZero_returnsTrue() {
            assertThat(NumberUtil.isZero(BigDecimal.ZERO)).isTrue();
            assertThat(NumberUtil.isZero(new BigDecimal("0.00"))).isTrue();
        }

        @Test
        @DisplayName("isZero - 非零值")
        void isZero_whenNonZero_returnsFalse() {
            assertThat(NumberUtil.isZero(new BigDecimal("1"))).isFalse();
            assertThat(NumberUtil.isZero(new BigDecimal("-1"))).isFalse();
            assertThat(NumberUtil.isZero(null)).isFalse();
        }

        @Test
        @DisplayName("isNonNegative - 非负数判断")
        void isNonNegative_whenNonNegative_returnsTrue() {
            assertThat(NumberUtil.isNonNegative(BigDecimal.ZERO)).isTrue();
            assertThat(NumberUtil.isNonNegative(new BigDecimal("1"))).isTrue();
        }

        @Test
        @DisplayName("isNonNegative - 负数")
        void isNonNegative_whenNegative_returnsFalse() {
            assertThat(NumberUtil.isNonNegative(new BigDecimal("-1"))).isFalse();
            assertThat(NumberUtil.isNonNegative(null)).isFalse();
        }

        @Test
        @DisplayName("max - 获取较大值")
        void max_whenTwoNumbers_returnsLarger() {
            assertThat(NumberUtil.max(
                new BigDecimal("10"),
                new BigDecimal("5")
            )).isEqualByComparingTo("10");
            
            assertThat(NumberUtil.max(
                new BigDecimal("5"),
                new BigDecimal("10")
            )).isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("max - null 处理")
        void max_whenNull_handlesCorrectly() {
            assertThat(NumberUtil.max(null, new BigDecimal("10")))
                .isEqualByComparingTo("10");
            assertThat(NumberUtil.max(new BigDecimal("10"), null))
                .isEqualByComparingTo("10");
            assertThat(NumberUtil.max(null, null)).isNull();
        }

        @Test
        @DisplayName("min - 获取较小值")
        void min_whenTwoNumbers_returnsSmaller() {
            assertThat(NumberUtil.min(
                new BigDecimal("10"),
                new BigDecimal("5")
            )).isEqualByComparingTo("5");
        }

        @Test
        @DisplayName("min - null 处理")
        void min_whenNull_handlesCorrectly() {
            assertThat(NumberUtil.min(null, new BigDecimal("10")))
                .isEqualByComparingTo("10");
            assertThat(NumberUtil.min(new BigDecimal("10"), null))
                .isEqualByComparingTo("10");
            assertThat(NumberUtil.min(null, null)).isNull();
        }
    }

    @Nested
    @DisplayName("单位转换")
    class UnitConversion {

        @Test
        @DisplayName("mmToPx - 毫米转像素")
        void mmToPx_whenValidValue_convertsCorrectly() {
            BigDecimal result = NumberUtil.mmToPx(new BigDecimal("25.4"), 96);
            assertThat(result).isEqualByComparingTo("96");
        }

        @Test
        @DisplayName("mmToPx - null 值返回 ZERO")
        void mmToPx_whenNull_returnsZero() {
            assertThat(NumberUtil.mmToPx(null, 96))
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("pxToMm - 像素转毫米")
        void pxToMm_whenValidValue_convertsCorrectly() {
            BigDecimal result = NumberUtil.pxToMm(new BigDecimal("96"), 96);
            assertThat(result).isEqualByComparingTo("25.40");
        }

        @Test
        @DisplayName("pxToMm - null 值或零 DPI 返回 ZERO")
        void pxToMm_whenNullOrZeroDpi_returnsZero() {
            assertThat(NumberUtil.pxToMm(null, 96))
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(NumberUtil.pxToMm(new BigDecimal("96"), 0))
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("中文数字转换")
    class ChineseNumberConversion {

        @Test
        @DisplayName("toChineseNumber - 零")
        void toChineseNumber_whenZero_returnsZero() {
            assertThat(NumberUtil.toChineseNumber(0)).isEqualTo("零");
        }

        @Test
        @DisplayName("toChineseNumber - 个位数")
        void toChineseNumber_whenSingleDigit_returnsCorrect() {
            assertThat(NumberUtil.toChineseNumber(1)).isEqualTo("一");
            assertThat(NumberUtil.toChineseNumber(9)).isEqualTo("九");
        }

        @Test
        @DisplayName("toChineseNumber - 十几")
        void toChineseNumber_whenTens_returnsCorrect() {
            assertThat(NumberUtil.toChineseNumber(10)).isEqualTo("十");
            assertThat(NumberUtil.toChineseNumber(11)).isEqualTo("十一");
            assertThat(NumberUtil.toChineseNumber(19)).isEqualTo("十九");
        }

        @Test
        @DisplayName("toChineseNumber - 几十")
        void toChineseNumber_whenMultipleTens_returnsCorrect() {
            assertThat(NumberUtil.toChineseNumber(20)).isEqualTo("二十");
            assertThat(NumberUtil.toChineseNumber(99)).isEqualTo("九十九");
        }

        @Test
        @DisplayName("toChineseNumber - 百位")
        void toChineseNumber_whenHundreds_returnsCorrect() {
            assertThat(NumberUtil.toChineseNumber(100)).isEqualTo("一百");
            assertThat(NumberUtil.toChineseNumber(101)).isEqualTo("一百零一");
            assertThat(NumberUtil.toChineseNumber(110)).isEqualTo("一百一十");
            assertThat(NumberUtil.toChineseNumber(999)).isEqualTo("九百九十九");
        }

        @Test
        @DisplayName("toChineseNumber - 千位")
        void toChineseNumber_whenThousands_returnsCorrect() {
            assertThat(NumberUtil.toChineseNumber(1000)).isEqualTo("一千");
            assertThat(NumberUtil.toChineseNumber(1001)).isEqualTo("一千零一");
            assertThat(NumberUtil.toChineseNumber(1010)).isEqualTo("一千零一十");
        }

        @Test
        @DisplayName("toChineseNumber - 万位")
        void toChineseNumber_whenTenThousands_returnsCorrect() {
            assertThat(NumberUtil.toChineseNumber(10000)).isEqualTo("一万");
            assertThat(NumberUtil.toChineseNumber(10001)).isEqualTo("一万零一");
            assertThat(NumberUtil.toChineseNumber(99999)).isEqualTo("九万九千九百九十九");
        }

        @Test
        @DisplayName("toChineseNumber - 超出范围抛出异常")
        void toChineseNumber_whenOutOfRange_throwsException() {
            assertThatThrownBy(() -> NumberUtil.toChineseNumber(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("数字超出范围");
            
            assertThatThrownBy(() -> NumberUtil.toChineseNumber(100000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("数字超出范围");
        }
    }

    @Nested
    @DisplayName("工具方法")
    class UtilityMethods {

        @Test
        @DisplayName("nullToZero - null 转 ZERO")
        void nullToZero_whenNull_returnsZero() {
            assertThat(NumberUtil.nullToZero(null))
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("nullToZero - 非 null 返回原值")
        void nullToZero_whenNotNull_returnsValue() {
            BigDecimal value = new BigDecimal("123.45");
            assertThat(NumberUtil.nullToZero(value)).isSameAs(value);
        }

        @Test
        @DisplayName("nullToDefault - null 转默认值")
        void nullToDefault_whenNull_returnsDefault() {
            BigDecimal defaultValue = new BigDecimal("999");
            assertThat(NumberUtil.nullToDefault(null, defaultValue))
                .isSameAs(defaultValue);
        }

        @Test
        @DisplayName("nullToDefault - 非 null 返回原值")
        void nullToDefault_whenNotNull_returnsValue() {
            BigDecimal value = new BigDecimal("123.45");
            assertThat(NumberUtil.nullToDefault(value, BigDecimal.ZERO))
                .isSameAs(value);
        }

        @Test
        @DisplayName("nullToDefault(Supplier) - null 时调用 Supplier")
        void nullToDefault_whenNullWithSupplier_callsSupplier() {
            BigDecimal result = NumberUtil.nullToDefault(null, () -> new BigDecimal("888"));
            assertThat(result).isEqualByComparingTo("888");
        }

        @Test
        @DisplayName("setScale - 设置小数位（四舍五入）")
        void setScale_whenValidValue_setsScale() {
            BigDecimal result = NumberUtil.setScale(new BigDecimal("123.456"), 2);
            assertThat(result).isEqualByComparingTo("123.46");
        }

        @Test
        @DisplayName("setScale - null 值返回 null")
        void setScale_whenNull_returnsNull() {
            assertThat(NumberUtil.setScale(null, 2)).isNull();
        }

        @Test
        @DisplayName("setScale - 指定舍入模式")
        void setScale_withRoundingMode_appliesMode() {
            BigDecimal result = NumberUtil.setScale(
                new BigDecimal("123.456"),
                2,
                RoundingMode.DOWN
            );
            assertThat(result).isEqualByComparingTo("123.45");
        }
    }
}
