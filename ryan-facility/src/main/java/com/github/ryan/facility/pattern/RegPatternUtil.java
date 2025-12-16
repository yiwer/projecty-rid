package com.github.ryan.facility.pattern;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <b>正则表达式工具类</b>
 * <p>
 * 提供高性能的正则匹配、提取、替换、分割等功能。
 * 所有 Pattern 自动缓存，避免重复编译。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *     <li><b>高性能</b>：Pattern 自动缓存，避免重复编译</li>
 *     <li><b>安全性</b>：提供 Optional 返回版本，避免 NPE</li>
 *     <li><b>易用性</b>：支持命名分组、Lambda 替换、流式 API</li>
 *     <li><b>完整性</b>：内置常用正则常量</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 校验
 * boolean valid = RegPatternUtil.isMatch(email, Patterns.EMAIL);
 *
 * // 提取
 * Optional<String> year = RegPatternUtil.findFirst(date, "(?<year>\\d{4})", "year");
 *
 * // 批量提取
 * List<String> urls = RegPatternUtil.findAll(html, Patterns.URL);
 *
 * // Lambda 替换
 * String result = RegPatternUtil.replaceAll(text, "\\d+", m -> String.valueOf(Integer.parseInt(m.group()) * 2));
 *
 * // 流式处理
 * RegPatternUtil.stream(text, regex).map(MatchResult::group).forEach(System.out::println);
 * }</pre>
 *
 * @author ryan
 * @since 1.0
 */
public final class RegPatternUtil {

    // ==================== 缓存 ====================

    /**
     * Pattern 缓存池
     */
    private static final Map<PatternKey, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private RegPatternUtil() {
    }

    /**
     * <b>获取/编译 Pattern</b>
     * <p>自动缓存，相同正则只编译一次。</p>
     *
     * @param regex 正则表达式
     *
     * @return 编译后的 Pattern
     *
     * @throws PatternSyntaxException 正则语法错误
     */
    public static Pattern compile(String regex) {
        return compile(regex, 0);
    }

    // ==================== Pattern 获取 ====================

    /**
     * <b>获取/编译 Pattern（带 Flag）</b>
     *
     * @param regex 正则表达式
     * @param flags 匹配标志，如 Pattern.CASE_INSENSITIVE
     */
    public static Pattern compile(String regex, int flags) {
        Objects.requireNonNull(regex, "regex cannot be null");
        return PATTERN_CACHE.computeIfAbsent(
                new PatternKey(regex, flags),
                k -> Pattern.compile(k.regex(), k.flags())
        );
    }

    /**
     * <b>安全编译 Pattern</b>
     * <p>语法错误时返回 empty。</p>
     */
    public static Optional<Pattern> tryCompile(String regex) {
        return tryCompile(regex, 0);
    }

    /**
     * <b>安全编译 Pattern（带 Flag）</b>
     */
    public static Optional<Pattern> tryCompile(String regex, int flags) {
        if (regex == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(compile(regex, flags));
        } catch (PatternSyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * <b>验证正则表达式语法是否有效</b>
     */
    public static boolean isValidRegex(String regex) {
        if (regex == null) {
            return false;
        }
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * <b>清空 Pattern 缓存</b>
     */
    public static void clearCache() {
        PATTERN_CACHE.clear();
    }

    /**
     * <b>获取缓存大小</b>
     */
    public static int cacheSize() {
        return PATTERN_CACHE.size();
    }

    /**
     * <b>是否完全匹配</b>
     * <p>整个字符串必须符合正则。</p>
     */
    public static boolean matches(String content, String regex) {
        if (content == null || regex == null) {
            return false;
        }
        return compile(regex).matcher(content).matches();
    }

    // ==================== 校验类 ====================

    /**
     * <b>是否完全匹配（忽略大小写）</b>
     */
    public static boolean matchesIgnoreCase(String content, String regex) {
        if (content == null || regex == null) {
            return false;
        }
        return compile(regex, Pattern.CASE_INSENSITIVE).matcher(content).matches();
    }

    /**
     * <b>是否包含匹配子串</b>
     */
    public static boolean contains(String content, String regex) {
        if (content == null || regex == null) {
            return false;
        }
        return compile(regex).matcher(content).find();
    }

    /**
     * <b>是否以匹配内容开头</b>
     */
    public static boolean startsWith(String content, String regex) {
        if (content == null || regex == null) {
            return false;
        }
        Matcher matcher = compile(regex).matcher(content);
        return matcher.find() && matcher.start() == 0;
    }

    /**
     * <b>是否以匹配内容结尾</b>
     */
    public static boolean endsWith(String content, String regex) {
        if (content == null || regex == null) {
            return false;
        }
        Matcher matcher = compile(regex).matcher(content);
        boolean found = false;
        int end = 0;
        while (matcher.find()) {
            found = true;
            end = matcher.end();
        }
        return found && end == content.length();
    }

    /**
     * <b>计算匹配次数</b>
     */
    public static int count(String content, String regex) {
        if (content == null || regex == null) {
            return 0;
        }
        Matcher matcher = compile(regex).matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * <b>提取第一个匹配的全文</b>
     */
    public static Optional<String> findFirst(String content, String regex) {
        return findFirst(content, regex, 0);
    }

    // ==================== 单次提取 ====================

    /**
     * <b>提取第一个匹配的指定分组</b>
     *
     * @param content    源字符串
     * @param regex      正则
     * @param groupIndex 分组下标（0=全文，1=第一个括号...)
     */
    public static Optional<String> findFirst(String content, String regex, int groupIndex) {
        if (content == null || regex == null) {
            return Optional.empty();
        }
        Matcher matcher = compile(regex).matcher(content);
        if (matcher.find() && groupIndex <= matcher.groupCount()) {
            return Optional.ofNullable(matcher.group(groupIndex));
        }
        return Optional.empty();
    }

    /**
     * <b>提取第一个匹配的命名分组</b>
     *
     * @param content   源字符串
     * @param regex     正则，如 "(?&lt;year&gt;\\d{4})-(?&lt;month&gt;\\d{2})"
     * @param groupName 分组名
     */
    public static Optional<String> findFirst(String content, String regex, String groupName) {
        if (content == null || regex == null || groupName == null) {
            return Optional.empty();
        }
        Matcher matcher = compile(regex).matcher(content);
        if (matcher.find()) {
            try {
                return Optional.ofNullable(matcher.group(groupName));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * <b>提取第一个匹配的所有分组</b>
     *
     * @return 分组列表，index 0 = group(1)，index 1 = group(2)...
     */
    public static List<String> findFirstGroups(String content, String regex) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        Matcher matcher = compile(regex).matcher(content);
        if (matcher.find()) {
            int count = matcher.groupCount();
            List<String> result = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                result.add(matcher.group(i));
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * <b>提取第一个匹配的所有命名分组为 Map</b>
     *
     * @param content    源字符串
     * @param regex      正则
     * @param groupNames 要提取的分组名列表
     */
    public static Map<String, String> findFirstAsMap(String content, String regex, String... groupNames) {
        if (content == null || regex == null || groupNames == null) {
            return Collections.emptyMap();
        }
        Matcher matcher = compile(regex).matcher(content);
        if (matcher.find()) {
            Map<String, String> result = new LinkedHashMap<>();
            for (String name : groupNames) {
                try {
                    result.put(name, matcher.group(name));
                } catch (IllegalArgumentException e) {
                    result.put(name, null);
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * <b>查找所有匹配项（返回全文）</b>
     */
    public static List<String> findAll(String content, String regex) {
        return findAll(content, regex, 0);
    }

    // ==================== 多次提取 ====================

    /**
     * <b>查找所有匹配项的指定分组</b>
     */
    public static List<String> findAll(String content, String regex, int groupIndex) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        Matcher matcher = compile(regex).matcher(content);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            if (groupIndex <= matcher.groupCount()) {
                result.add(matcher.group(groupIndex));
            }
        }
        return result;
    }

    /**
     * <b>查找所有匹配项的命名分组</b>
     */
    public static List<String> findAll(String content, String regex, String groupName) {
        if (content == null || regex == null || groupName == null) {
            return Collections.emptyList();
        }
        Matcher matcher = compile(regex).matcher(content);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            try {
                result.add(matcher.group(groupName));
            } catch (IllegalArgumentException e) {
                // 分组名不存在，跳过
            }
        }
        return result;
    }

    /**
     * <b>查找所有匹配项的所有分组</b>
     *
     * @return List&lt;List&lt;String&gt;&gt;，外层是每次匹配，内层是该次匹配的各个分组
     */
    public static List<List<String>> findAllGroups(String content, String regex) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        Matcher matcher = compile(regex).matcher(content);
        List<List<String>> result = new ArrayList<>();
        while (matcher.find()) {
            int count = matcher.groupCount();
            List<String> groups = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                groups.add(matcher.group(i));
            }
            result.add(groups);
        }
        return result;
    }

    /**
     * <b>查找所有匹配项，返回命名分组 Map 列表</b>
     */
    public static List<Map<String, String>> findAllAsMap(String content, String regex, String... groupNames) {
        if (content == null || regex == null || groupNames == null) {
            return Collections.emptyList();
        }
        Matcher matcher = compile(regex).matcher(content);
        List<Map<String, String>> result = new ArrayList<>();
        while (matcher.find()) {
            Map<String, String> map = new LinkedHashMap<>();
            for (String name : groupNames) {
                try {
                    map.put(name, matcher.group(name));
                } catch (IllegalArgumentException e) {
                    map.put(name, null);
                }
            }
            result.add(map);
        }
        return result;
    }

    /**
     * <b>查找所有不重复的匹配项</b>
     */
    public static Set<String> findDistinct(String content, String regex) {
        return findDistinct(content, regex, 0);
    }

    /**
     * <b>查找所有不重复的匹配项（指定分组）</b>
     */
    public static Set<String> findDistinct(String content, String regex, int groupIndex) {
        if (content == null || regex == null) {
            return Collections.emptySet();
        }
        Matcher matcher = compile(regex).matcher(content);
        Set<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            if (groupIndex <= matcher.groupCount()) {
                result.add(matcher.group(groupIndex));
            }
        }
        return result;
    }

    /**
     * <b>替换第一个匹配项</b>
     */
    public static String replaceFirst(String content, String regex, String replacement) {
        if (content == null || regex == null) {
            return content;
        }
        return compile(regex).matcher(content).replaceFirst(replacement != null ? replacement : "");
    }

    // ==================== 替换 ====================

    /**
     * <b>替换所有匹配项</b>
     */
    public static String replaceAll(String content, String regex, String replacement) {
        if (content == null || regex == null) {
            return content;
        }
        return compile(regex).matcher(content).replaceAll(replacement != null ? replacement : "");
    }

    /**
     * <b>Lambda 替换第一个匹配项</b>
     *
     * @param content   源字符串
     * @param regex     正则
     * @param converter 回调函数，入参是 Matcher，返回替换后的字符串
     */
    public static String replaceFirst(String content, String regex, Function<Matcher, String> converter) {
        if (content == null || regex == null || converter == null) {
            return content;
        }
        Matcher matcher = compile(regex).matcher(content);
        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            String replacement = converter.apply(matcher);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement != null ? replacement : ""));
            matcher.appendTail(sb);
            return sb.toString();
        }
        return content;
    }

    /**
     * <b>Lambda 替换所有匹配项</b>
     *
     * @param content   源字符串
     * @param regex     正则
     * @param converter 回调函数，入参是 Matcher，返回替换后的字符串
     */
    public static String replaceAll(String content, String regex, Function<Matcher, String> converter) {
        if (content == null || regex == null || converter == null) {
            return content;
        }
        Matcher matcher = compile(regex).matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replacement = converter.apply(matcher);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement != null ? replacement : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * <b>删除所有匹配项</b>
     */
    public static String remove(String content, String regex) {
        return replaceAll(content, regex, "");
    }

    /**
     * <b>删除第一个匹配项</b>
     */
    public static String removeFirst(String content, String regex) {
        return replaceFirst(content, regex, "");
    }

    /**
     * <b>按正则分割字符串</b>
     */
    public static List<String> split(String content, String regex) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(compile(regex).split(content));
    }

    // ==================== 分割 ====================

    /**
     * <b>按正则分割字符串，限制结果数量</b>
     *
     * @param limit 最大分割数，0 表示无限
     */
    public static List<String> split(String content, String regex, int limit) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(compile(regex).split(content, limit));
    }

    /**
     * <b>分割并过滤空字符串</b>
     */
    public static List<String> splitNonEmpty(String content, String regex) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(compile(regex).split(content))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * <b>分割并去除空白</b>
     */
    public static List<String> splitTrimmed(String content, String regex) {
        if (content == null || regex == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(compile(regex).split(content))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * <b>获取匹配结果流</b>
     * <p>可用于流式处理匹配结果。</p>
     *
     * <pre>{@code
     * stream(text, regex)
     *     .map(MatchResult::group)
     *     .filter(s -> s.length() > 3)
     *     .forEach(System.out::println);
     * }</pre>
     */
    public static Stream<MatchResult> stream(String content, String regex) {
        if (content == null || regex == null) {
            return Stream.empty();
        }
        return compile(regex).matcher(content).results();
    }

    // ==================== 流式 API ====================

    /**
     * <b>获取匹配结果流（带 Flag）</b>
     */
    public static Stream<MatchResult> stream(String content, String regex, int flags) {
        if (content == null || regex == null) {
            return Stream.empty();
        }
        return compile(regex, flags).matcher(content).results();
    }

    /**
     * <b>转义正则特殊字符</b>
     * <p>将字符串中的正则元字符转义，使其作为字面量匹配。</p>
     */
    public static String escape(String literal) {
        return literal == null ? null : Pattern.quote(literal);
    }

    // ==================== 转义与引用 ====================

    /**
     * <b>转义替换字符串中的特殊字符</b>
     * <p>将 $ 和 \ 转义，用于 appendReplacement。</p>
     */
    public static String escapeReplacement(String replacement) {
        return replacement == null ? null : Matcher.quoteReplacement(replacement);
    }

    /**
     * <b>验证邮箱格式</b>
     */
    public static boolean isEmail(String str) {
        return matches(str, Patterns.EMAIL);
    }

    // ==================== 常用正则常量 ====================

    /**
     * <b>验证手机号格式（中国大陆）</b>
     */
    public static boolean isMobileCN(String str) {
        return matches(str, Patterns.MOBILE_CN);
    }

    // ==================== 常用验证快捷方法 ====================

    /**
     * <b>验证身份证号格式</b>
     */
    public static boolean isIdCard(String str) {
        return matches(str, Patterns.ID_CARD_18) || matches(str, Patterns.ID_CARD_15);
    }

    /**
     * <b>验证 URL 格式</b>
     */
    public static boolean isUrl(String str) {
        return matches(str, Patterns.URL);
    }

    /**
     * <b>验证 IPv4 地址</b>
     */
    public static boolean isIpv4(String str) {
        return matches(str, Patterns.IPV4);
    }

    /**
     * <b>验证是否为整数</b>
     */
    public static boolean isInteger(String str) {
        return matches(str, Patterns.INTEGER);
    }

    /**
     * <b>验证是否为数字</b>
     */
    public static boolean isNumber(String str) {
        return matches(str, Patterns.NUMBER);
    }

    /**
     * <b>验证是否包含中文</b>
     */
    public static boolean containsChinese(String str) {
        return contains(str, Patterns.CHINESE);
    }

    /**
     * <b>验证 UUID 格式</b>
     */
    public static boolean isUuid(String str) {
        return matches(str, Patterns.UUID);
    }


    /**
     * 缓存键
     */
    private record PatternKey(String regex, int flags) {
    }

    // ==================== 字符串历史兼容 ====================

    /**
     * <b>常用正则表达式常量</b>
     */
    public static final class Patterns {

        /**
         * 邮箱地址
         */
        public static final String EMAIL = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

        // ===== 网络相关 =====
        /**
         * URL
         */
        public static final String URL = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+";
        /**
         * IPv4 地址
         */
        public static final String IPV4 = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        /**
         * IPv6 地址（简化版）
         */
        public static final String IPV6 = "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}";
        /**
         * 域名
         */
        public static final String DOMAIN = "[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+";
        /**
         * 手机号（中国大陆）
         */
        public static final String MOBILE_CN = "1[3-9]\\d{9}";

        // ===== 中国特色 =====
        /**
         * 身份证号（18位）
         */
        public static final String ID_CARD_18 = "[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]";
        /**
         * 身份证号（15位）
         */
        public static final String ID_CARD_15 = "[1-9]\\d{5}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}";
        /**
         * 中文字符
         */
        public static final String CHINESE = "[\\u4e00-\\u9fa5]";
        /**
         * 中文字符串（一个或多个）
         */
        public static final String CHINESE_WORDS = "[\\u4e00-\\u9fa5]+";
        /**
         * 邮政编码（中国）
         */
        public static final String ZIP_CODE_CN = "[1-9]\\d{5}";
        /**
         * 车牌号（中国）
         */
        public static final String LICENSE_PLATE_CN = "[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏宁琴使A-Z][A-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]";
        /**
         * 整数
         */
        public static final String INTEGER = "-?[0-9]+";

        // ===== 数字相关 =====
        /**
         * 正整数
         */
        public static final String POSITIVE_INTEGER = "[1-9]\\d*";
        /**
         * 负整数
         */
        public static final String NEGATIVE_INTEGER = "-[1-9]\\d*";
        /**
         * 小数
         */
        public static final String DECIMAL = "-?[0-9]+\\.[0-9]+";
        /**
         * 数字（整数或小数）
         */
        public static final String NUMBER = "-?[0-9]+(\\.[0-9]+)?";
        /**
         * 金额（最多两位小数）
         */
        public static final String MONEY = "-?[0-9]+(\\.[0-9]{1,2})?";
        /**
         * 日期 yyyy-MM-dd
         */
        public static final String DATE = "\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])";

        // ===== 日期时间 =====
        /**
         * 时间 HH:mm:ss
         */
        public static final String TIME = "(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d";
        /**
         * 日期时间 yyyy-MM-dd HH:mm:ss
         */
        public static final String DATETIME = DATE + " " + TIME;
        /**
         * 弱密码（6-20位字母数字）
         */
        public static final String PASSWORD_WEAK = "[a-zA-Z0-9]{6,20}";

        // ===== 安全相关 =====
        /**
         * 强密码（包含大小写字母、数字、特殊字符，8-20位）
         */
        public static final String PASSWORD_STRONG = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,20}$";
        /**
         * 变量名（字母、数字、下划线，不能数字开头）
         */
        public static final String VARIABLE_NAME = "[a-zA-Z_][a-zA-Z0-9_]*";

        // ===== 编程相关 =====
        /**
         * UUID
         */
        public static final String UUID = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        /**
         * 十六进制颜色值
         */
        public static final String HEX_COLOR = "#?([0-9a-fA-F]{6}|[0-9a-fA-F]{3})";
        /**
         * 空白字符
         */
        public static final String WHITESPACE = "\\s+";
        /**
         * 非空白字符
         */
        public static final String NON_WHITESPACE = "\\S+";
        /**
         * HTML 标签
         */
        public static final String HTML_TAG = "<[^>]+>";

        // ===== HTML 相关 =====
        /**
         * HTML 注释
         */
        public static final String HTML_COMMENT = "<!--[\\s\\S]*?-->";

        private Patterns() {
        }

        /**
         * 匹配指定 HTML 标签内容
         */
        public static String htmlTagContent(String tagName) {
            return "<" + tagName + "[^>]*>([\\s\\S]*?)</" + tagName + ">";
        }

        /**
         * 匹配指定属性值
         */
        public static String htmlAttribute(String attrName) {
            return attrName + "=[\"']([^\"']*)[\"']";
        }
    }


}