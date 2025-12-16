package com.github.ryan.facility.number;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <b>数字工具类</b>
 * <p>
 * 提供数字格式化、安全解析、单位转换、中文数字转换等功能。
 * 所有方法均为 null 安全，解析方法返回 Optional 避免异常。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *     <li><b>null 安全</b>：所有格式化方法 null 输入返回空字符串</li>
 *     <li><b>安全解析</b>：解析方法返回 Optional，不抛出异常</li>
 *     <li><b>不可变</b>：所有方法不修改输入参数</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 格式化为2位小数
 * String str = NumberUtil.format(new BigDecimal("123.456"), 2); // "123.46"
 *
 * // 安全解析
 * Optional<BigDecimal> num = NumberUtil.parseBigDecimal("123.45");
 * BigDecimal value = NumberUtil.parseBigDecimalOrDefault("invalid", BigDecimal.ZERO);
 *
 * // 金额格式化（千分位）
 * String money = NumberUtil.formatMoney(new BigDecimal("1234567.89")); // "1,234,567.89"
 *
 * // 数字转中文
 * String chinese = NumberUtil.toChineseNumber(12); // "十二"
 *
 * // 比较
 * boolean isPositive = NumberUtil.isPositive(amount);
 * boolean equals = NumberUtil.equals(a, b);
 * }</pre>
 *
 * @author yvvb
 * @since 2025/5/22
 */
@UtilityClass
public class NumberUtil {

    // ==================== 常量 ====================

    /**
     * 一英寸对应的毫米数
     */
    private static final double MM_PER_INCH = 25.4;

    /**
     * 中文数字字符
     */
    private static final String[] CHINESE_DIGITS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };

    /**
     * 中文数字单位
     */
    private static final String[] CHINESE_UNITS = {"", "十", "百", "千", "万"};

    // ==================== 格式化器工厂 ====================

    /**
     * 创建 DecimalFormat（非线程安全，每次新建）
     */
    private static DecimalFormat createFormat(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }


    // ==================== 格式化方法 ====================

    /**
     * <b>格式化数字为指定小数位</b>
     *
     * @param value 数字值（可为 null）
     * @param scale 小数位数
     *
     * @return 格式化后的字符串，null 返回空字符串
     */
    public static String format(BigDecimal value, int scale) {
        if (value == null) {
            return "";
        }
        String pattern = scale <= 0 ? "#0" : "#0." + "0".repeat(scale);
        return createFormat(pattern).format(value);
    }

    /**
     * <b>格式化数字为整数</b>
     *
     * @param value 数字值（可为 null）
     *
     * @return 格式化后的字符串
     */
    public static String formatInt(Number value) {
        if (value == null) {
            return "";
        }
        return createFormat("#0").format(value);
    }

    /**
     * <b>格式化数字为 2 位小数</b>
     *
     * @param value 数字值（可为 null）
     *
     * @return 格式化后的字符串
     */
    public static String format2(BigDecimal value) {
        return format(value, 2);
    }

    /**
     * <b>格式化数字为 4 位小数</b>
     *
     * @param value 数字值（可为 null）
     *
     * @return 格式化后的字符串
     */
    public static String format4(BigDecimal value) {
        return format(value, 4);
    }

    /**
     * <b>智能格式化小数</b>
     * <p>根据小数位数自动选择合适的格式，去除尾部多余的零</p>
     *
     * @param value BigDecimal 值（可为 null）
     *
     * @return 格式化后的字符串
     */
    public static String formatSmart(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        int scale = Math.max(0, stripped.scale());
        if (scale == 0) {
            return stripped.toPlainString();
        }
        return format(value, Math.min(scale, 8));
    }

    /**
     * <b>格式化为金额（千分位 + 2位小数）</b>
     *
     * @param value 金额值（可为 null）
     *
     * @return 格式化后的字符串，如 "1,234,567.89"
     */
    public static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return createFormat("#,##0.00").format(value);
    }

    /**
     * <b>格式化为百分比</b>
     *
     * @param value 小数值（可为 null），如0.1234 表示 12.34%
     * @param scale 小数位数
     *
     * @return 格式化后的字符串，如 "12.34%"
     */
    public static String formatPercent(BigDecimal value, int scale) {
        if (value == null) {
            return "";
        }
        BigDecimal percent = value.multiply(BigDecimal.valueOf(100));
        return format(percent, scale) + "%";
    }

    // ==================== 安全解析方法 ====================

    /**
     * <b>安全解析为 BigDecimal</b>
     *
     * @param str 字符串
     *
     * @return Optional 包装的 BigDecimal
     */
    public static Optional<BigDecimal> parseBigDecimal(String str) {
        if (str == null || str.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(str.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * <b>解析为 BigDecimal，失败返回默认值</b>
     *
     * @param str          字符串
     * @param defaultValue 默认值
     *
     * @return BigDecimal 值
     */
    public static BigDecimal parseBigDecimalOrDefault(String str, BigDecimal defaultValue) {
        return parseBigDecimal(str).orElse(defaultValue);
    }

    /**
     * <b>解析为 BigDecimal，失败返回 ZERO</b>
     *
     * @param str 字符串
     *
     * @return BigDecimal 值
     */
    public static BigDecimal parseBigDecimalOrZero(String str) {
        return parseBigDecimalOrDefault(str, BigDecimal.ZERO);
    }

    /**
     * <b>安全解析为 Integer</b>
     *
     * @param str 字符串
     *
     * @return Optional 包装的 Integer
     */
    public static Optional<Integer> parseInt(String str) {
        if (str == null || str.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(str.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * <b>解析为 Integer，失败返回默认值</b>
     *
     * @param str          字符串
     * @param defaultValue 默认值
     *
     * @return Integer 值
     */
    public static int parseIntOrDefault(String str, int defaultValue) {
        return parseInt(str).orElse(defaultValue);
    }

    /**
     * <b>安全解析为 Long</b>
     *
     * @param str 字符串
     *
     * @return Optional 包装的 Long
     */
    public static Optional<Long> parseLong(String str) {
        if (str == null || str.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(str.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * <b>解析为 Long，失败返回默认值</b>
     *
     * @param str          字符串
     * @param defaultValue 默认值
     *
     * @return Long 值
     */
    public static long parseLongOrDefault(String str, long defaultValue) {
        return parseLong(str).orElse(defaultValue);
    }

    /**
     * <b>安全解析为 Double</b>
     *
     * @param str 字符串
     *
     * @return Optional 包装的 Double
     */
    public static Optional<Double> parseDouble(String str) {
        if (str == null || str.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(str.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // ==================== 比较方法 ====================

    /**
     * <b>比较两个 BigDecimal 是否相等</b>
     * <p>null 安全，两个 null 视为相等</p>
     *
     * @param a 第一个数
     * @param b 第二个数
     *
     * @return 是否相等
     */
    public static boolean equals(BigDecimal a, BigDecimal b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    /**
     * <b>判断是否为正数</b>
     *
     * @param value 数字值
     *
     * @return 大于 0 返回 true
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * <b>判断是否为负数</b>
     *
     * @param value 数字值
     *
     * @return 小于 0 返回 true
     */
    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * <b>判断是否为零</b>
     *
     * @param value 数字值
     *
     * @return 等于 0 返回 true
     */
    public static boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * <b>判断是否为非负数</b>
     *
     * @param value 数字值
     *
     * @return 大于等于 0 返回 true
     */
    public static boolean isNonNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * <b>获取较大值</b>
     *
     * @param a 第一个数
     * @param b 第二个数
     *
     * @return 较大值，null 安全
     */
    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * <b>获取较小值</b>
     *
     * @param a 第一个数
     * @param b 第二个数
     *
     * @return 较小值，null 安全
     */
    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) <= 0 ? a : b;
    }

    // ==================== 单位转换 ====================

    /**
     * <b>毫米转像素</b>
     * <p>根据 DPI 计算毫米对应的像素值，公式：px = mm * dpi / 25.4</p>
     *
     * @param mm  毫米值（可为 null）
     * @param dpi 每英寸像素数
     *
     * @return 像素值，null 返回 0
     */
    public static BigDecimal mmToPx(BigDecimal mm, int dpi) {
        if (mm == null) {
            return BigDecimal.ZERO;
        }
        double px = mm.doubleValue() * dpi / MM_PER_INCH;
        return BigDecimal.valueOf(Math.round(px));
    }

    /**
     * <b>像素转毫米</b>
     *
     * @param px  像素值（可为 null）
     * @param dpi 每英寸像素数
     *
     * @return 毫米值，null 返回 0
     */
    public static BigDecimal pxToMm(BigDecimal px, int dpi) {
        if (px == null || dpi == 0) {
            return BigDecimal.ZERO;
        }
        double mm = px.doubleValue() * MM_PER_INCH / dpi;
        return BigDecimal.valueOf(mm).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 中文数字转换 ====================

    /**
     * <b>将阿拉伯数字转换为中文数字</b>
     * <p>支持 0-99999 的数字转换，遵循中文习惯（如 11-19 不显示"一十"）</p>
     *
     * @param num 阿拉伯数字（0-99999）
     *
     * @return 中文数字字符串
     *
     * @throws IllegalArgumentException 数字超出范围时抛出
     */
    public static String toChineseNumber(int num) {
        if (num < 0 || num > 99999) {
            throw new IllegalArgumentException("数字超出范围（0-99999）");
        }

        if (num == 0) return CHINESE_DIGITS[0];

        StringBuilder sb = new StringBuilder();
        boolean needZero = false;

        // 万位
        if (num >= 10000) {
            sb.append(CHINESE_DIGITS[num / 10000]).append(CHINESE_UNITS[4]);
            num %= 10000;
            needZero = num > 0 && num < 1000;
        }

        // 千位
        if (num >= 1000) {
            if (needZero) sb.append(CHINESE_DIGITS[0]);
            sb.append(CHINESE_DIGITS[num / 1000]).append(CHINESE_UNITS[3]);
            num %= 1000;
            needZero = num > 0 && num < 100;
        }

        // 百位
        if (num >= 100) {
            if (needZero) sb.append(CHINESE_DIGITS[0]);
            sb.append(CHINESE_DIGITS[num / 100]).append(CHINESE_UNITS[2]);
            num %= 100;
            needZero = num > 0 && num < 10;
        }

        // 十位
        if (num >= 10) {
            if (needZero) sb.append(CHINESE_DIGITS[0]);
            int tens = num / 10;
            // 十几的特殊处理，且不是一十几千/一十几万的情况
            if (tens == 1 && sb.isEmpty()) {
                sb.append(CHINESE_UNITS[1]);
            } else {
                sb.append(CHINESE_DIGITS[tens]).append(CHINESE_UNITS[1]);
            }
            num %= 10;
            needZero = false;
        }

        // 个位
        if (num > 0) {
            if (needZero) sb.append(CHINESE_DIGITS[0]);
            sb.append(CHINESE_DIGITS[num]);
        }

        return sb.toString();
    }

    // ==================== 工具方法 ====================

    /**
     * <b>安全获取 BigDecimal，null 返回 ZERO</b>
     *
     * @param value 数字值
     *
     * @return 非 null 的 BigDecimal
     */
    public static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * <b>安全获取 BigDecimal，null 返回指定默认值</b>
     *
     * @param value        数字值
     * @param defaultValue 默认值
     *
     * @return 非 null 的 BigDecimal
     */
    public static BigDecimal nullToDefault(BigDecimal value, BigDecimal defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * <b>安全获取 BigDecimal，null 时通过 Supplier 获取默认值</b>
     *
     * @param value    数字值
     * @param supplier 默认值提供者
     *
     * @return 非 null 的 BigDecimal
     */
    public static BigDecimal nullToDefault(BigDecimal value, Supplier<BigDecimal> supplier) {
        return value != null ? value : supplier.get();
    }

    /**
     * <b>设置小数位数（四舍五入）</b>
     *
     * @param value 数字值
     * @param scale 小数位数
     *
     * @return 设置后的 BigDecimal，null 返回 null
     */
    public static BigDecimal setScale(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * <b>设置小数位数（指定舍入模式）</b>
     *
     * @param value        数字值
     * @param scale        小数位数
     * @param roundingMode 舍入模式
     *
     * @return 设置后的 BigDecimal，null 返回 null
     */
    public static BigDecimal setScale(BigDecimal value, int scale, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, roundingMode);
    }
}
