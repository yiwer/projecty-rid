package com.github.ryan.facility.http.client.model;

import com.github.ryan.facility.error.FacilityErrorType;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.json.JsonUtil;
import com.github.ryan.facility.result.Result;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * <b>HTTP 请求构建器</b>
 * <p>
 * 提供流畅的链式 API 构建 HTTP 请求，支持各种请求方式和请求体类型。
 * 设计灵感来自 Rust 的 reqwest 库。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // GET 请求
 * HttpRequest request = HttpRequestBuilder.get("https://api.example.com/users")
 *     .query("page", 1)
 *     .query("size", 10)
 *     .header("Authorization", "Bearer token")
 *     .build();
 *
 * // POST JSON 请求
 * HttpRequest request = HttpRequestBuilder.post("https://api.example.com/users")
 *     .json(user)
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // POST 表单请求
 * HttpRequest request = HttpRequestBuilder.post("https://api.example.com/login")
 *     .form("username", "admin")
 *     .form("password", "123456")
 *     .build();
 * }</pre>
 *
 * @author yvvb
 * @since 2025/5/4
 */
public final class HttpRequestBuilder {

    // ==================== 常量 ====================

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json; charset=utf-8";
    private static final String APPLICATION_FORM = "application/x-www-form-urlencoded";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String TEXT_PLAIN = "text/plain; charset=utf-8";

    // ==================== 字段 ====================

    private final String method;
    private final String baseUrl;
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> formParams = new LinkedHashMap<>();
    private HttpRequest.BodyPublisher bodyPublisher;
    private String contentType;
    private Duration timeout;

    private HttpRequestBuilder(String method, String baseUrl) {
        this.method = Objects.requireNonNull(method, "HTTP method cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "URL cannot be null");
    }

    // ==================== 工厂方法 ====================

    /**
     * <b>创建 GET 请求构建器</b>
     *
     * @param url 请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder get(String url) {
        return new HttpRequestBuilder("GET", url);
    }

    /**
     * <b>创建 POST 请求构建器</b>
     *
     * @param url 请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder post(String url) {
        return new HttpRequestBuilder("POST", url);
    }

    /**
     * <b>创建 PUT 请求构建器</b>
     *
     * @param url 请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder put(String url) {
        return new HttpRequestBuilder("PUT", url);
    }

    /**
     * <b>创建 DELETE 请求构建器</b>
     *
     * @param url 请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder delete(String url) {
        return new HttpRequestBuilder("DELETE", url);
    }

    /**
     * <b>创建 PATCH 请求构建器</b>
     *
     * @param url 请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder patch(String url) {
        return new HttpRequestBuilder("PATCH", url);
    }

    /**
     * <b>创建 HEAD 请求构建器</b>
     *
     * @param url 请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder head(String url) {
        return new HttpRequestBuilder("HEAD", url);
    }

    /**
     * <b>创建自定义方法的请求构建器</b>
     *
     * @param method HTTP 方法
     * @param url    请求 URL
     * @return HttpRequestBuilder
     */
    public static HttpRequestBuilder method(String method, String url) {
        return new HttpRequestBuilder(method.toUpperCase(), url);
    }

    // ==================== 查询参数 ====================

    /**
     * <b>添加查询参数</b>
     *
     * @param name  参数名
     * @param value 参数值
     * @return this
     */
    public HttpRequestBuilder query(String name, Object value) {
        if (name != null && value != null) {
            queryParams.put(name, String.valueOf(value));
        }
        return this;
    }

    /**
     * <b>批量添加查询参数</b>
     *
     * @param params 参数 Map
     * @return this
     */
    public HttpRequestBuilder queries(Map<String, ?> params) {
        if (params != null) {
            params.forEach((k, v) -> query(k, v));
        }
        return this;
    }

    // ==================== 请求头 ====================

    /**
     * <b>添加请求头</b>
     *
     * @param name  头名称
     * @param value 头值
     * @return this
     */
    public HttpRequestBuilder header(String name, String value) {
        if (name != null && value != null) {
            headers.put(name, value);
        }
        return this;
    }

    /**
     * <b>批量添加请求头</b>
     *
     * @param headerMap 请求头 Map
     * @return this
     */
    public HttpRequestBuilder headers(Map<String, String> headerMap) {
        if (headerMap != null) {
            headers.putAll(headerMap);
        }
        return this;
    }

    /**
     * <b>设置 Bearer Token</b>
     *
     * @param token JWT Token
     * @return this
     */
    public HttpRequestBuilder bearerAuth(String token) {
        return header("Authorization", "Bearer " + token);
    }

    /**
     * <b>设置 Basic Auth</b>
     *
     * @param username 用户名
     * @param password 密码
     * @return this
     */
    public HttpRequestBuilder basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return header("Authorization", "Basic " + encoded);
    }

    /**
     * <b>设置 Accept 头</b>
     *
     * @param mediaType 媒体类型
     * @return this
     */
    public HttpRequestBuilder accept(String mediaType) {
        return header("Accept", mediaType);
    }

    // ==================== 请求体 ====================

    /**
     * <b>设置 JSON 请求体</b>
     *
     * @param body 要序列化为 JSON 的对象
     * @return this
     */
    public HttpRequestBuilder json(Object body) {
        if (body == null) {
            this.bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else if (body instanceof String str) {
            this.bodyPublisher = HttpRequest.BodyPublishers.ofString(str);
        } else {
            String json = JsonUtil.serialize(body).orElse("{}");
            this.bodyPublisher = HttpRequest.BodyPublishers.ofString(json);
        }
        this.contentType = APPLICATION_JSON;
        return this;
    }

    /**
     * <b>添加表单参数</b>
     *
     * @param name  参数名
     * @param value 参数值
     * @return this
     */
    public HttpRequestBuilder form(String name, Object value) {
        if (name != null && value != null) {
            formParams.put(name, String.valueOf(value));
        }
        this.contentType = APPLICATION_FORM;
        return this;
    }

    /**
     * <b>批量添加表单参数</b>
     *
     * @param params 参数 Map
     * @return this
     */
    public HttpRequestBuilder forms(Map<String, ?> params) {
        if (params != null) {
            params.forEach((k, v) -> form(k, v));
        }
        return this;
    }

    /**
     * <b>设置纯文本请求体</b>
     *
     * @param text 文本内容
     * @return this
     */
    public HttpRequestBuilder text(String text) {
        this.bodyPublisher = HttpRequest.BodyPublishers.ofString(text != null ? text : "");
        this.contentType = TEXT_PLAIN;
        return this;
    }

    /**
     * <b>设置字节数组请求体</b>
     *
     * @param bytes 字节数组
     * @return this
     */
    public HttpRequestBuilder bytes(byte[] bytes) {
        this.bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(bytes != null ? bytes : new byte[0]);
        this.contentType = APPLICATION_OCTET_STREAM;
        return this;
    }

    /**
     * <b>设置文件请求体</b>
     *
     * @param path 文件路径
     * @return this
     */
    public HttpRequestBuilder file(Path path) {
        try {
            this.bodyPublisher = HttpRequest.BodyPublishers.ofFile(path);
            this.contentType = APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            this.bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }
        return this;
    }

    /**
     * <b>设置输入流请求体</b>
     *
     * @param inputStream 输入流
     * @return this
     */
    public HttpRequestBuilder stream(InputStream inputStream) {
        this.bodyPublisher = HttpRequest.BodyPublishers.ofInputStream(() -> inputStream);
        this.contentType = APPLICATION_OCTET_STREAM;
        return this;
    }

    /**
     * <b>设置输入流提供者请求体</b>
     *
     * @param streamSupplier 输入流提供者
     * @return this
     */
    public HttpRequestBuilder stream(Supplier<InputStream> streamSupplier) {
        this.bodyPublisher = HttpRequest.BodyPublishers.ofInputStream(streamSupplier);
        this.contentType = APPLICATION_OCTET_STREAM;
        return this;
    }

    /**
     * <b>设置自定义请求体</b>
     *
     * @param publisher   BodyPublisher
     * @param contentType Content-Type
     * @return this
     */
    public HttpRequestBuilder body(HttpRequest.BodyPublisher publisher, String contentType) {
        this.bodyPublisher = publisher;
        this.contentType = contentType;
        return this;
    }

    // ==================== 其他配置 ====================

    /**
     * <b>设置请求超时时间</b>
     *
     * @param timeout 超时时间
     * @return this
     */
    public HttpRequestBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    // ==================== 构建 ====================

    /**
     * <b>构建 HttpRequest</b>
     *
     * @return Result 包含 HttpRequest 或错误信息
     */
    public Result<HttpRequest, WrappedError> build() {
        try {
            // 构建 URI
            URI uri = buildUri();

            // 构建请求
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri);

            // 设置方法和请求体
            HttpRequest.BodyPublisher finalBody = resolveBodyPublisher();
            builder.method(method, finalBody);

            // 设置请求头
            if (contentType != null) {
                builder.header(CONTENT_TYPE, contentType);
            }
            headers.forEach(builder::header);

            // 设置超时
            if (timeout != null) {
                builder.timeout(timeout);
            }

            return Result.ok(builder.build());
        } catch (Exception e) {
            return Result.err(WrappedError.of(FacilityErrorType.BUILD_HTTP_REQUEST_BODY_ERROR, e));
        }
    }

    /**
     * <b>构建 HttpRequest（不安全版本）</b>
     *
     * @return HttpRequest
     * @throws RuntimeException 构建失败时
     */
    public HttpRequest buildUnsafe() {
        return build().orElseThrow(err ->
                new RuntimeException("Failed to build HttpRequest", err.getException()));
    }

    // ==================== 内部方法 ====================

    /**
     * 构建带查询参数的 URI
     */
    private URI buildUri() {
        if (queryParams.isEmpty()) {
            return URI.create(baseUrl);
        }

        StringJoiner joiner = new StringJoiner("&");
        queryParams.forEach((k, v) -> {
            String encodedKey = URLEncoder.encode(k, StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(v, StandardCharsets.UTF_8);
            joiner.add(encodedKey + "=" + encodedValue);
        });

        String separator = baseUrl.contains("?") ? "&" : "?";
        return URI.create(baseUrl + separator + joiner);
    }

    /**
     * 解析最终的 BodyPublisher
     */
    private HttpRequest.BodyPublisher resolveBodyPublisher() {
        // 如果已经设置了 bodyPublisher，直接使用
        if (bodyPublisher != null) {
            return bodyPublisher;
        }

        // 如果有表单参数，构建表单请求体
        if (!formParams.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            formParams.forEach((k, v) -> {
                String encodedKey = URLEncoder.encode(k, StandardCharsets.UTF_8);
                String encodedValue = URLEncoder.encode(v, StandardCharsets.UTF_8);
                joiner.add(encodedKey + "=" + encodedValue);
            });
            return HttpRequest.BodyPublishers.ofString(joiner.toString());
        }

        // 无请求体
        return HttpRequest.BodyPublishers.noBody();
    }
}
