package com.github.ryan.facility.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ryan.facility.error.FacilityErrorType;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.json.support.JsonConfig;
import com.github.ryan.facility.log.LogUtil;
import com.github.ryan.facility.result.Result;

import com.github.ryan.facility.json.support.TypeRef;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>JSON 序列化/反序列化工具类</b>
 * <p>
 * 基于 Jackson 提供统一的 JSON 操作接口，设计灵感来自 Rust 的 serde 库。
 * 所有操作返回 {@link Result} 类型，支持链式错误处理。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *     <li><b>Result 驱动</b>：所有操作返回 Result，强制调用方处理错误</li>
 *     <li><b>错误可追溯</b>：WrappedError 包含错误类型、原始异常、上下文参数</li>
 *     <li><b>泛型友好</b>：完整支持复杂泛型类型的反序列化</li>
 *     <li><b>多策略支持</b>：通过命名空间机制支持多种序列化配置</li>
 * </ul>
 *
 * <h3>内置序列化器：</h3>
 * <ul>
 *     <li><b>default</b> - 标准配置，适合大多数场景</li>
 *     <li><b>generic</b> - 通用配置，使用标准 Java 时间模块</li>
 *     <li><b>canonical</b> - 规范化配置，属性按字母序排列，用于比较/签名</li>
 *     <li><b>pretty</b> - 美化输出配置，便于调试</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 序列化
 * Result<String, WrappedError> result = JsonUtil.serialize(user);
 * String json = result.orElse("{}");
 *
 * // 反序列化 - 简单类型
 * Result<User, WrappedError> userResult = JsonUtil.deserialize(json, User.class);
 *
 * // 反序列化 - 泛型类型
 * Result<List<User>, WrappedError> listResult = JsonUtil.deserialize(
 *     json,
 *     new TypeReference<List<User>>() {}
 * );
 *
 * // 链式处理
 * String name = JsonUtil.deserialize(json, User.class)
 *     .map(User::getName)
 *     .orElse("unknown");
 *
 * // 使用特定序列化器
 * JsonUtil.use(JsonUtil.PRETTY).serialize(object);
 *
 * // 不安全模式（确定不会失败时使用）
 * String json = JsonUtil.serializeUnsafe(simpleObject);
 * }</pre>
 *
 * @author yvvb
 * @since 2025/4/17
 * @see Result
 * @see WrappedError
 * @see JsonConfig
 */
public final class JsonUtil {

    // ==================== 命名空间常量 ====================

    /**
     * 默认序列化器命名空间
     */
    public static final String DEFAULT = "default";

    /**
     * 通用序列化器命名空间
     */
    public static final String GENERIC = "generic";

    /**
     * 规范化序列化器命名空间（字母序属性）
     */
    public static final String CANONICAL = "canonical";

    /**
     * 美化输出序列化器命名空间
     */
    public static final String PRETTY = "pretty";

    // ==================== 内部状态 ====================

    /**
     * 命名空间到执行器的映射
     */
    private static final Map<String, Serializer> SERIALIZER_REGISTRY = new ConcurrentHashMap<>();

    /**
     * 默认序列化器（延迟初始化）
     */
    private static volatile Serializer defaultSerializer;

    // ==================== 静态初始化 ====================

    static {
        // 注册内置序列化器
        registerBuiltinSerializers();
    }

    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtil() {
    }

    // ==================== 序列化方法 ====================

    /**
     * <b>将对象序列化为 JSON 字符串</b>
     *
     * @param value 要序列化的对象
     * @return {@link Result} 包含 JSON 字符串或错误信息
     */
    public static Result<String, WrappedError> serialize(Object value) {
        return getDefault().serialize(value);
    }

    /**
     * <b>将对象序列化为字节数组</b>
     *
     * @param value 要序列化的对象
     * @return {@link Result} 包含字节数组或错误信息
     */
    public static Result<byte[], WrappedError> serializeToBytes(Object value) {
        return getDefault().serializeToBytes(value);
    }

    /**
     * <b>将对象序列化并写入输出流</b>
     *
     * @param value  要序列化的对象
     * @param output 输出流
     * @return {@link Result} 成功返回 Void，失败返回错误信息
     */
    public static Result<Void, WrappedError> serializeTo(Object value, OutputStream output) {
        return getDefault().serializeTo(value, output);
    }

    /**
     * <b>不安全的序列化（失败时抛出异常）</b>
     * <p>适用于确定不会失败的简单场景，如日志输出</p>
     *
     * @param value 要序列化的对象
     * @return JSON 字符串
     * @throws JsonSerializationException 序列化失败时
     */
    public static String serializeUnsafe(Object value) {
        return serialize(value).orElseThrow(err ->
                new JsonSerializationException("Serialize failed", err.getException()));
    }

    /**
     * <b>不安全的字节数组序列化</b>
     *
     * @param value 要序列化的对象
     * @return 字节数组
     * @throws JsonSerializationException 序列化失败时
     */
    public static byte[] serializeToBytesUnsafe(Object value) {
        return serializeToBytes(value).orElseThrow(err ->
                new JsonSerializationException("Serialize to bytes failed", err.getException()));
    }

    // ==================== 反序列化方法 ====================

    /**
     * <b>将 JSON 字符串反序列化为指定类型</b>
     *
     * @param json   JSON 字符串
     * @param target 目标类型
     * @param <T>    目标类型泛型
     * @return {@link Result} 包含反序列化对象或错误信息
     */
    public static <T> Result<T, WrappedError> deserialize(String json, Class<T> target) {
        return getDefault().deserialize(json, target);
    }

    /**
     * <b>将 JSON 字符串反序列化为泛型类型</b>
     * <p>用于处理带泛型的类型，如 {@code List<User>}、{@code Map<String, Object>}</p>
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return {@link Result} 包含反序列化对象或错误信息
     */
    public static <T> Result<T, WrappedError> deserialize(String json, TypeReference<T> typeReference) {
        return getDefault().deserialize(json, typeReference);
    }

    /**
     * <b>将字节数组反序列化为指定类型</b>
     *
     * @param bytes  字节数组
     * @param target 目标类型
     * @param <T>    目标类型泛型
     * @return {@link Result} 包含反序列化对象或错误信息
     */
    public static <T> Result<T, WrappedError> deserialize(byte[] bytes, Class<T> target) {
        return getDefault().deserialize(bytes, target);
    }

    /**
     * <b>将字节数组反序列化为泛型类型</b>
     *
     * @param bytes         字节数组
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return {@link Result} 包含反序列化对象或错误信息
     */
    public static <T> Result<T, WrappedError> deserialize(byte[] bytes, TypeReference<T> typeReference) {
        return getDefault().deserialize(bytes, typeReference);
    }

    /**
     * <b>从输入流反序列化为指定类型</b>
     *
     * @param input  输入流
     * @param target 目标类型
     * @param <T>    目标类型泛型
     * @return {@link Result} 包含反序列化对象或错误信息
     */
    public static <T> Result<T, WrappedError> deserialize(InputStream input, Class<T> target) {
        return getDefault().deserialize(input, target);
    }

    /**
     * <b>从输入流反序列化为泛型类型</b>
     *
     * @param input         输入流
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return {@link Result} 包含反序列化对象或错误信息
     */
    public static <T> Result<T, WrappedError> deserialize(InputStream input, TypeReference<T> typeReference) {
        return getDefault().deserialize(input, typeReference);
    }

    /**
     * <b>不安全的反序列化（失败时抛出异常）</b>
     *
     * @param json   JSON 字符串
     * @param target 目标类型
     * @param <T>    目标类型泛型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化失败时
     */
    public static <T> T deserializeUnsafe(String json, Class<T> target) {
        return deserialize(json, target).orElseThrow(err ->
                new JsonDeserializationException("Deserialize failed", err.getException()));
    }

    /**
     * <b>不安全的泛型反序列化</b>
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化对象
     * @throws JsonDeserializationException 反序列化失败时
     */
    public static <T> T deserializeUnsafe(String json, TypeReference<T> typeReference) {
        return deserialize(json, typeReference).orElseThrow(err ->
                new JsonDeserializationException("Deserialize failed", err.getException()));
    }

    // ==================== 便捷反序列化方法 ====================

    /**
     * <b>反序列化为 List</b>
     *
     * @param json         JSON 字符串
     * @param elementClass 列表元素类型
     * @param <E>          元素类型泛型
     * @return {@link Result} 包含 List 或错误信息
     */
    public static <E> Result<List<E>, WrappedError> deserializeToList(String json, Class<E> elementClass) {
        return getDefault().deserializeToList(json, elementClass);
    }

    /**
     * <b>反序列化为 Set</b>
     *
     * @param json         JSON 字符串
     * @param elementClass 集合元素类型
     * @param <E>          元素类型泛型
     * @return {@link Result} 包含 Set 或错误信息
     */
    public static <E> Result<Set<E>, WrappedError> deserializeToSet(String json, Class<E> elementClass) {
        return getDefault().deserializeToSet(json, elementClass);
    }

    /**
     * <b>反序列化为 Map</b>
     *
     * @param json       JSON 字符串
     * @param keyClass   键类型
     * @param valueClass 值类型
     * @param <K>        键类型泛型
     * @param <V>        值类型泛型
     * @return {@link Result} 包含 Map 或错误信息
     */
    public static <K, V> Result<Map<K, V>, WrappedError> deserializeToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        return getDefault().deserializeToMap(json, keyClass, valueClass);
    }

    /**
     * <b>反序列化为 Map&lt;String, V&gt;</b>
     * <p>常用的字符串键 Map 快捷方法</p>
     *
     * @param json       JSON 字符串
     * @param valueClass 值类型
     * @param <V>        值类型泛型
     * @return {@link Result} 包含 Map 或错误信息
     */
    public static <V> Result<Map<String, V>, WrappedError> deserializeToStringMap(String json, Class<V> valueClass) {
        return deserialize(json, TypeRef.ofStringMap(valueClass));
    }

    /**
     * <b>反序列化为 Map&lt;String, List&lt;V&gt;&gt;</b>
     *
     * @param json       JSON 字符串
     * @param valueClass 列表元素类型
     * @param <V>        元素类型泛型
     * @return {@link Result} 包含 Map 或错误信息
     */
    public static <V> Result<Map<String, List<V>>, WrappedError> deserializeToStringListMap(String json, Class<V> valueClass) {
        return deserialize(json, TypeRef.ofStringListMap(valueClass));
    }

    /**
     * <b>反序列化为 List&lt;Map&lt;String, V&gt;&gt;</b>
     *
     * @param json       JSON 字符串
     * @param valueClass Map 值类型
     * @param <V>        值类型泛型
     * @return {@link Result} 包含 List 或错误信息
     */
    public static <V> Result<List<Map<String, V>>, WrappedError> deserializeToListStringMap(String json, Class<V> valueClass) {
        return deserialize(json, TypeRef.ofListStringMap(valueClass));
    }

    // ==================== JsonNode 操作 ====================

    /**
     * <b>将 JSON 字符串解析为 JsonNode</b>
     *
     * @param json JSON 字符串
     * @return {@link Result} 包含 JsonNode 或错误信息
     */
    public static Result<JsonNode, WrappedError> parseTree(String json) {
        return getDefault().parseTree(json);
    }

    /**
     * <b>将对象转换为 JsonNode</b>
     *
     * @param value 要转换的对象
     * @return {@link Result} 包含 JsonNode 或错误信息
     */
    public static Result<JsonNode, WrappedError> valueToTree(Object value) {
        return getDefault().valueToTree(value);
    }

    /**
     * <b>将 JsonNode 转换为指定类型</b>
     *
     * @param node   JsonNode
     * @param target 目标类型
     * @param <T>    目标类型泛型
     * @return {@link Result} 包含对象或错误信息
     */
    public static <T> Result<T, WrappedError> treeToValue(JsonNode node, Class<T> target) {
        return getDefault().treeToValue(node, target);
    }

    // ==================== 序列化器管理 ====================

    /**
     * <b>获取指定命名空间的序列化器</b>
     *
     * @param namespace 命名空间名称
     * @return 序列化器，不存在则返回 null
     */
    public static Serializer use(String namespace) {
        return SERIALIZER_REGISTRY.get(namespace);
    }

    /**
     * <b>注册自定义序列化器</b>
     *
     * @param namespace    命名空间
     * @param objectMapper ObjectMapper 实例
     * @return true-注册成功，false-参数无效
     */
    public static boolean register(String namespace, ObjectMapper objectMapper) {
        if (namespace == null || namespace.isBlank() || objectMapper == null) {
            return false;
        }
        SERIALIZER_REGISTRY.put(namespace, new Serializer(objectMapper));
        return true;
    }

    /**
     * <b>获取默认 ObjectMapper 实例</b>
     * <p>用于需要直接操作 ObjectMapper 的场景</p>
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper mapper() {
        return getDefault().mapper();
    }

    // ==================== 内部方法 ====================

    /**
     * 获取默认序列化器（双重检查锁定）
     */
    private static Serializer getDefault() {
        if (defaultSerializer == null) {
            synchronized (JsonUtil.class) {
                if (defaultSerializer == null) {
                    defaultSerializer = SERIALIZER_REGISTRY.get(DEFAULT);
                }
            }
        }
        return defaultSerializer;
    }

    /**
     * 注册内置序列化器
     */
    private static void registerBuiltinSerializers() {
        // 默认配置
        SERIALIZER_REGISTRY.put(DEFAULT, new Serializer(
                JsonConfig.standard().useDefaultDateFormat().build()
        ));

        // 通用配置
        SERIALIZER_REGISTRY.put(GENERIC, new Serializer(
                JsonConfig.standard().build()
        ));

        // 规范化配置
        SERIALIZER_REGISTRY.put(CANONICAL, new Serializer(
                JsonConfig.canonical().build()
        ));

        // 美化输出配置
        SERIALIZER_REGISTRY.put(PRETTY, new Serializer(
                JsonConfig.prettyPrint().useDefaultDateFormat().build()
        ));
    }

    // ==================== 序列化器类 ====================

    /**
     * <b>JSON 序列化器</b>
     * <p>
     * 封装 ObjectMapper 的 JSON 操作，提供类型安全的序列化/反序列化方法。
     * 所有方法返回 {@link Result} 类型，支持链式错误处理。
     * </p>
     */
    public static class Serializer {

        private final ObjectMapper objectMapper;

        public Serializer(ObjectMapper objectMapper) {
            this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        }

        /**
         * 获取内部 ObjectMapper 实例
         */
        public ObjectMapper mapper() {
            return objectMapper;
        }

        // -------------------- 序列化 --------------------

        /**
         * 序列化为 JSON 字符串
         */
        public Result<String, WrappedError> serialize(Object value) {
            try {
                return Result.ok(objectMapper.writeValueAsString(value));
            } catch (JsonProcessingException e) {
                return handleSerializeError(e, value);
            }
        }

        /**
         * 序列化为字节数组
         */
        public Result<byte[], WrappedError> serializeToBytes(Object value) {
            try {
                return Result.ok(objectMapper.writeValueAsBytes(value));
            } catch (JsonProcessingException e) {
                return handleSerializeError(e, value);
            }
        }

        /**
         * 序列化并写入输出流
         */
        public Result<Void, WrappedError> serializeTo(Object value, OutputStream output) {
            try {
                objectMapper.writeValue(output, value);
                return Result.ok();
            } catch (IOException e) {
                return handleSerializeError(e, value);
            }
        }

        // -------------------- 反序列化 --------------------

        /**
         * 从字符串反序列化
         */
        public <T> Result<T, WrappedError> deserialize(String json, Class<T> target) {
            try {
                return Result.ok(objectMapper.readValue(json, target));
            } catch (JsonProcessingException e) {
                return handleDeserializeError(e, json, target.getName());
            }
        }

        /**
         * 从字符串反序列化（泛型）
         */
        public <T> Result<T, WrappedError> deserialize(String json, TypeReference<T> typeReference) {
            try {
                return Result.ok(objectMapper.readValue(json, typeReference));
            } catch (JsonProcessingException e) {
                return handleDeserializeError(e, json, typeReference.getType().getTypeName());
            }
        }

        /**
         * 从字节数组反序列化
         */
        public <T> Result<T, WrappedError> deserialize(byte[] bytes, Class<T> target) {
            try {
                return Result.ok(objectMapper.readValue(bytes, target));
            } catch (IOException e) {
                return handleDeserializeError(e, "[bytes]", target.getName());
            }
        }

        /**
         * 从字节数组反序列化（泛型）
         */
        public <T> Result<T, WrappedError> deserialize(byte[] bytes, TypeReference<T> typeReference) {
            try {
                return Result.ok(objectMapper.readValue(bytes, typeReference));
            } catch (IOException e) {
                return handleDeserializeError(e, "[bytes]", typeReference.getType().getTypeName());
            }
        }

        /**
         * 从输入流反序列化
         */
        public <T> Result<T, WrappedError> deserialize(InputStream input, Class<T> target) {
            try {
                return Result.ok(objectMapper.readValue(input, target));
            } catch (IOException e) {
                return handleDeserializeError(e, "[stream]", target.getName());
            }
        }

        /**
         * 从输入流反序列化（泛型）
         */
        public <T> Result<T, WrappedError> deserialize(InputStream input, TypeReference<T> typeReference) {
            try {
                return Result.ok(objectMapper.readValue(input, typeReference));
            } catch (IOException e) {
                return handleDeserializeError(e, "[stream]", typeReference.getType().getTypeName());
            }
        }

        // -------------------- 便捷反序列化 --------------------

        /**
         * 反序列化为 List
         */
        public <E> Result<List<E>, WrappedError> deserializeToList(String json, Class<E> elementClass) {
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(ArrayList.class, elementClass);
            return deserializeWithJavaType(json, type);
        }

        /**
         * 反序列化为 Set
         */
        public <E> Result<Set<E>, WrappedError> deserializeToSet(String json, Class<E> elementClass) {
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(LinkedHashSet.class, elementClass);
            return deserializeWithJavaType(json, type);
        }

        /**
         * 反序列化为 Map
         */
        public <K, V> Result<Map<K, V>, WrappedError> deserializeToMap(String json, Class<K> keyClass, Class<V> valueClass) {
            JavaType type = objectMapper.getTypeFactory()
                    .constructMapType(LinkedHashMap.class, keyClass, valueClass);
            return deserializeWithJavaType(json, type);
        }

        /**
         * 反序列化为 Map&lt;String, V&gt;
         */
        public <V> Result<Map<String, V>, WrappedError> deserializeToStringMap(String json, Class<V> valueClass) {
            return deserialize(json, TypeRef.ofStringMap(valueClass));
        }

        /**
         * 反序列化为 Map&lt;String, List&lt;V&gt;&gt;
         */
        public <V> Result<Map<String, List<V>>, WrappedError> deserializeToStringListMap(String json, Class<V> valueClass) {
            return deserialize(json, TypeRef.ofStringListMap(valueClass));
        }

        /**
         * 反序列化为 List&lt;Map&lt;String, V&gt;&gt;
         */
        public <V> Result<List<Map<String, V>>, WrappedError> deserializeToListStringMap(String json, Class<V> valueClass) {
            return deserialize(json, TypeRef.ofListStringMap(valueClass));
        }

        /**
         * 使用 JavaType 反序列化
         */
        private <T> Result<T, WrappedError> deserializeWithJavaType(String json, JavaType javaType) {
            try {
                return Result.ok(objectMapper.readValue(json, javaType));
            } catch (JsonProcessingException e) {
                return handleDeserializeError(e, json, javaType.getTypeName());
            }
        }

        // -------------------- JsonNode 操作 --------------------

        /**
         * 解析为 JsonNode
         */
        public Result<JsonNode, WrappedError> parseTree(String json) {
            try {
                return Result.ok(objectMapper.readTree(json));
            } catch (JsonProcessingException e) {
                return handleDeserializeError(e, json, "JsonNode");
            }
        }

        /**
         * 对象转 JsonNode
         */
        public Result<JsonNode, WrappedError> valueToTree(Object value) {
            try {
                return Result.ok(objectMapper.valueToTree(value));
            } catch (IllegalArgumentException e) {
                LogUtil.error(e, "Convert to JsonNode failed, value: {}", safeToString(value));
                return Result.err(WrappedError.of(
                        FacilityErrorType.JSON_NODE_TRANSFER_ERROR,
                        new RuntimeException(e)
                ));
            }
        }

        /**
         * JsonNode 转对象
         */
        public <T> Result<T, WrappedError> treeToValue(JsonNode node, Class<T> target) {
            try {
                return Result.ok(objectMapper.treeToValue(node, target));
            } catch (JsonProcessingException e) {
                LogUtil.error(e, "Convert JsonNode to value failed, target: {}", target.getName());
                return Result.err(WrappedError.of(
                        FacilityErrorType.JSON_NODE_TRANSFER_ERROR,
                        e
                ));
            }
        }

        // -------------------- 错误处理 --------------------

        /**
         * 处理序列化错误
         */
        private <T> Result<T, WrappedError> handleSerializeError(Exception e, Object value) {
            String valueStr = safeToString(value);
            LogUtil.error(e, "JSON serialize failed, value: {}", valueStr);
            return Result.err(WrappedError.of(
                    FacilityErrorType.JSON_SERIALIZE_ERROR,
                    e,
                    new Object[]{valueStr}
            ));
        }

        /**
         * 处理反序列化错误
         */
        private <T> Result<T, WrappedError> handleDeserializeError(Exception e, String source, String targetType) {
            String truncatedSource = truncate(source, 500);
            LogUtil.error(e, "JSON deserialize failed, target: {}, source: {}", targetType, truncatedSource);
            return Result.err(WrappedError.of(
                    FacilityErrorType.JSON_DESERIALIZE_ERROR,
                    e,
                    new Object[]{targetType, truncatedSource}
            ));
        }

        /**
         * 安全的 toString（防止 toString 抛异常）
         */
        private String safeToString(Object obj) {
            if (obj == null) {
                return "null";
            }
            try {
                String str = obj.toString();
                return truncate(str, 200);
            } catch (Exception e) {
                return obj.getClass().getName() + "@" + System.identityHashCode(obj);
            }
        }

        /**
         * 截断字符串
         */
        private String truncate(String str, int maxLen) {
            if (str == null) {
                return "null";
            }
            if (str.length() <= maxLen) {
                return str;
            }
            return str.substring(0, maxLen) + "...(truncated, total: " + str.length() + ")";
        }
    }

    // ==================== 异常类 ====================

    /**
     * JSON 序列化异常
     */
    public static class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * JSON 反序列化异常
     */
    public static class JsonDeserializationException extends RuntimeException {
        public JsonDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
