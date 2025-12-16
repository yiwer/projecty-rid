package com.github.ryan.facility.http.client.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.ryan.facility.error.FacilityErrorType;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.json.JsonUtil;
import com.github.ryan.facility.result.Result;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>HTTP 响应封装</b>
 * <p>
 * 封装 HTTP 响应的所有信息，提供便捷的响应体解析方法。
 * 设计灵感来自 Rust 的 reqwest Response。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 发送请求并获取响应
 * Result<HttpResult, WrappedError> result = Http.send(request);
 *
 * // 获取字符串响应
 * String body = result.get().text();
 *
 * // 解析为 JSON 对象
 * Result<User, WrappedError> user = result.get().json(User.class);
 *
 * // 检查状态码
 * if (result.get().isSuccess()) {
 *     // 2xx 状态码
 * }
 * }</pre>
 *
 * @author yvvb
 * @since 2025/5/4
 */
public final class HttpResult {

    // ==================== 字段 ====================

    private final int statusCode;
    private final HttpHeaders headers;
    private final byte[] body;
    private final String bodyText;

    // ==================== 构造函数 ====================

    private HttpResult(int statusCode, HttpHeaders headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.bodyText = body != null ? new String(body, StandardCharsets.UTF_8) : "";
    }

    // ==================== 工厂方法 ====================

    /**
     * <b>从 HttpResponse 创建 HttpResult</b>
     *
     * @param response 原始响应
     * @return HttpResult
     */
    public static HttpResult from(HttpResponse<byte[]> response) {
        return new HttpResult(
                response.statusCode(),
                response.headers(),
                response.body()
        );
    }

    /**
     * <b>从字符串响应创建 HttpResult</b>
     *
     * @param response 原始响应
     * @return HttpResult
     */
    public static HttpResult fromString(HttpResponse<String> response) {
        String body = response.body();
        return new HttpResult(
                response.statusCode(),
                response.headers(),
                body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0]
        );
    }

    // ==================== 状态查询 ====================

    /**
     * <b>获取状态码</b>
     *
     * @return HTTP 状态码
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * <b>是否成功 (2xx)</b>
     *
     * @return true 表示 2xx 状态码
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * <b>是否为 OK (200)</b>
     *
     * @return true 表示 200 状态码
     */
    public boolean isOk() {
        return statusCode == 200;
    }

    /**
     * <b>是否为客户端错误 (4xx)</b>
     *
     * @return true 表示 4xx 状态码
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * <b>是否为服务端错误 (5xx)</b>
     *
     * @return true 表示 5xx 状态码
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    // ==================== 响应头 ====================

    /**
     * <b>获取所有响应头</b>
     *
     * @return 响应头 Map
     */
    public Map<String, List<String>> headers() {
        return headers.map();
    }

    /**
     * <b>获取指定响应头的第一个值</b>
     *
     * @param name 头名称
     * @return 头值，不存在返回 empty
     */
    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }

    /**
     * <b>获取指定响应头的所有值</b>
     *
     * @param name 头名称
     * @return 头值列表
     */
    public List<String> headerValues(String name) {
        return headers.allValues(name);
    }

    /**
     * <b>获取 Content-Type</b>
     *
     * @return Content-Type 值
     */
    public Optional<String> contentType() {
        return header("Content-Type");
    }

    // ==================== 响应体 ====================

    /**
     * <b>获取原始字节数组</b>
     *
     * @return 响应体字节数组
     */
    public byte[] bytes() {
        return body != null ? body : new byte[0];
    }

    /**
     * <b>获取字符串响应体</b>
     *
     * @return 响应体字符串
     */
    public String text() {
        return bodyText;
    }

    /**
     * <b>解析为 JSON 对象</b>
     *
     * @param clazz 目标类型
     * @param <T>   类型泛型
     * @return Result 包含解析结果或错误
     */
    public <T> Result<T, WrappedError> json(Class<T> clazz) {
        if (bodyText == null || bodyText.isBlank()) {
            return Result.err(WrappedError.of(FacilityErrorType.JSON_DESERIALIZE_ERROR));
        }
        return JsonUtil.deserialize(bodyText, clazz);
    }

    /**
     * <b>解析为泛型 JSON 对象</b>
     *
     * @param typeReference 类型引用
     * @param <T>           类型泛型
     * @return Result 包含解析结果或错误
     */
    public <T> Result<T, WrappedError> json(TypeReference<T> typeReference) {
        if (bodyText == null || bodyText.isBlank()) {
            return Result.err(WrappedError.of(FacilityErrorType.JSON_DESERIALIZE_ERROR));
        }
        return JsonUtil.deserialize(bodyText, typeReference);
    }

    /**
     * <b>解析为 JSON 列表</b>
     *
     * @param elementClass 元素类型
     * @param <E>          元素类型泛型
     * @return Result 包含解析结果或错误
     */
    public <E> Result<List<E>, WrappedError> jsonList(Class<E> elementClass) {
        if (bodyText == null || bodyText.isBlank()) {
            return Result.err(WrappedError.of(FacilityErrorType.JSON_DESERIALIZE_ERROR));
        }
        return JsonUtil.deserializeToList(bodyText, elementClass);
    }

    /**
     * <b>解析为 JSON Map</b>
     *
     * @param keyClass   键类型
     * @param valueClass 值类型
     * @param <K>        键类型泛型
     * @param <V>        值类型泛型
     * @return Result 包含解析结果或错误
     */
    public <K, V> Result<Map<K, V>, WrappedError> jsonMap(Class<K> keyClass, Class<V> valueClass) {
        if (bodyText == null || bodyText.isBlank()) {
            return Result.err(WrappedError.of(FacilityErrorType.JSON_DESERIALIZE_ERROR));
        }
        return JsonUtil.deserializeToMap(bodyText, keyClass, valueClass);
    }

    // ==================== 转换方法 ====================

    /**
     * <b>要求成功，否则返回错误</b>
     * <p>如果状态码不是 2xx，返回错误结果</p>
     *
     * @return Result 包含自身或错误
     */
    public Result<HttpResult, WrappedError> requireSuccess() {
        if (isSuccess()) {
            return Result.ok(this);
        }
        return Result.err(WrappedError.of(
                FacilityErrorType.HTTP_SEND_AND_PARSE_ERROR,
                new RuntimeException("HTTP request failed with status: " + statusCode + ", body: " + truncateBody())
        ));
    }

    /**
     * <b>要求状态码等于指定值</b>
     *
     * @param expectedStatus 期望的状态码
     * @return Result 包含自身或错误
     */
    public Result<HttpResult, WrappedError> requireStatus(int expectedStatus) {
        if (statusCode == expectedStatus) {
            return Result.ok(this);
        }
        return Result.err(WrappedError.of(
                FacilityErrorType.HTTP_SEND_AND_PARSE_ERROR,
                new RuntimeException("Expected status " + expectedStatus + ", got " + statusCode)
        ));
    }

    // ==================== 内部方法 ====================

    private String truncateBody() {
        if (bodyText == null) return "";
        if (bodyText.length() <= 500) return bodyText;
        return bodyText.substring(0, 500) + "...(truncated)";
    }

    @Override
    public String toString() {
        return "HttpResult{" +
                "statusCode=" + statusCode +
                ", bodyLength=" + (body != null ? body.length : 0) +
                '}';
    }
}
