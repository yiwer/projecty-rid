package com.github.ryan.facility.http.client;

import com.github.ryan.facility.error.FacilityErrorType;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.http.client.model.HttpClientConfig;
import com.github.ryan.facility.http.client.model.HttpRequestBuilder;
import com.github.ryan.facility.http.client.model.HttpResult;
import com.github.ryan.facility.log.LogUtil;
import com.github.ryan.facility.result.Result;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <b>HTTP 客户端工具类</b>
 * <p>
 * 对 Java HttpClient 的封装，提供简洁的 HTTP 请求 API。
 * 设计灵感来自 Rust 的 reqwest 库，所有操作返回 {@link Result} 类型。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *     <li><b>Result 驱动</b>：所有操作返回 Result，强制调用方处理错误</li>
 *     <li><b>链式 API</b>：流畅的请求构建和响应处理</li>
 *     <li><b>异步支持</b>：提供同步和异步两种发送方式</li>
 *     <li><b>可配置</b>：支持自定义客户端配置</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 简单 GET 请求
 * Result<HttpResult, WrappedError> result = HttpUtil.get("https://api.example.com/users")
 *     .query("page", 1)
 *     .send();
 *
 * // POST JSON 请求
 * Result<HttpResult, WrappedError> result = HttpUtil.post("https://api.example.com/users")
 *     .json(user)
 *     .bearerAuth(token)
 *     .send();
 *
 * // 链式处理响应
 * User user = HttpUtil.get("https://api.example.com/users/1")
 *     .send()
 *     .flatMap(HttpResult::requireSuccess)
 *     .flatMap(r -> r.json(User.class))
 *     .orElse(null);
 *
 * // 异步请求
 * CompletableFuture<Result<HttpResult, WrappedError>> future = HttpUtil.get(url).sendAsync();
 *
 * // 自定义客户端
 * HttpClient client = HttpClientConfig.create()
 *     .connectTimeout(Duration.ofSeconds(30))
 *     .proxy("proxy.example.com", 8080)
 *     .build();
 * HttpUtil.withClient(client).get(url).send();
 * }</pre>
 *
 * @author yvvb
 * @see HttpRequestBuilder
 * @see HttpResult
 * @see HttpClientConfig
 * @since 2025/5/4
 */
public final class HttpUtil {

    // ==================== 默认客户端 ====================

    /**
     * 默认 HTTP 客户端实例
     */
    private static final HttpClient DEFAULT_CLIENT = HttpClientConfig.defaultClient();

    /**
     * 默认请求超时时间
     */
    private static final Duration DEFAULT_TIMEOUT = HttpClientConfig.DEFAULT_REQUEST_TIMEOUT;

    private HttpUtil() {
    }

    // ==================== 快捷方法 ====================

    /**
     * <b>创建 GET 请求</b>
     *
     * @param url 请求 URL
     *
     * @return RequestExecutor
     */
    public static RequestExecutor get(String url) {
        return new RequestExecutor(DEFAULT_CLIENT, HttpRequestBuilder.get(url), DEFAULT_TIMEOUT);
    }

    /**
     * <b>创建 POST 请求</b>
     *
     * @param url 请求 URL
     *
     * @return RequestExecutor
     */
    public static RequestExecutor post(String url) {
        return new RequestExecutor(DEFAULT_CLIENT, HttpRequestBuilder.post(url), DEFAULT_TIMEOUT);
    }

    /**
     * <b>创建 PUT 请求</b>
     *
     * @param url 请求 URL
     *
     * @return RequestExecutor
     */
    public static RequestExecutor put(String url) {
        return new RequestExecutor(DEFAULT_CLIENT, HttpRequestBuilder.put(url), DEFAULT_TIMEOUT);
    }

    /**
     * <b>创建 DELETE 请求</b>
     *
     * @param url 请求 URL
     *
     * @return RequestExecutor
     */
    public static RequestExecutor delete(String url) {
        return new RequestExecutor(DEFAULT_CLIENT, HttpRequestBuilder.delete(url), DEFAULT_TIMEOUT);
    }

    /**
     * <b>创建 PATCH 请求</b>
     *
     * @param url 请求 URL
     *
     * @return RequestExecutor
     */
    public static RequestExecutor patch(String url) {
        return new RequestExecutor(DEFAULT_CLIENT, HttpRequestBuilder.patch(url), DEFAULT_TIMEOUT);
    }

    /**
     * <b>使用自定义客户端</b>
     *
     * @param client HttpClient 实例
     *
     * @return ClientContext
     */
    public static ClientContext withClient(HttpClient client) {
        return new ClientContext(client, DEFAULT_TIMEOUT);
    }

    /**
     * <b>使用自定义配置</b>
     *
     * @param config 客户端配置
     *
     * @return ClientContext
     */
    public static ClientContext withConfig(HttpClientConfig config) {
        return new ClientContext(config.build(), config.getRequestTimeout());
    }

    /**
     * <b>发送已构建的请求</b>
     *
     * @param request HttpRequest
     *
     * @return Result 包含 HttpResult 或错误
     */
    public static Result<HttpResult, WrappedError> send(HttpRequest request) {
        return send(DEFAULT_CLIENT, request);
    }

    /**
     * <b>使用指定客户端发送请求</b>
     *
     * @param client  HttpClient
     * @param request HttpRequest
     *
     * @return Result 包含 HttpResult 或错误
     */
    public static Result<HttpResult, WrappedError> send(HttpClient client, HttpRequest request) {
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return Result.ok(HttpResult.from(response));
        } catch (IOException e) {
            LogUtil.error(e, "HTTP request failed: {}", request.uri());
            return Result.err(WrappedError.of(FacilityErrorType.HTTP_SEND_AND_PARSE_ERROR, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogUtil.warn(e, "HTTP request interrupted: {}", request.uri());
            return Result.err(WrappedError.of(FacilityErrorType.HTTP_SEND_AND_PARSE_ERROR, e));
        }
    }

    /**
     * <b>异步发送请求</b>
     *
     * @param client  HttpClient
     * @param request HttpRequest
     *
     * @return CompletableFuture
     */
    public static CompletableFuture<Result<HttpResult, WrappedError>> sendAsync(HttpClient client, HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> Result.<HttpResult, WrappedError>ok(HttpResult.from(response)))
                .exceptionally(e -> {
                    LogUtil.error((Exception) e, "Async HTTP request failed: {}", request.uri());
                    return Result.err(WrappedError.of(FacilityErrorType.HTTP_SEND_AND_PARSE_ERROR, (Exception) e));
                });
    }

    // ==================== 客户端上下文 ====================

    /**
     * <b>客户端上下文</b>
     * <p>用于在特定客户端上执行请求</p>
     */
    public static class ClientContext {
        private final HttpClient client;
        private final Duration defaultTimeout;

        ClientContext(HttpClient client, Duration defaultTimeout) {
            this.client = Objects.requireNonNull(client);
            this.defaultTimeout = defaultTimeout;
        }

        public RequestExecutor get(String url) {
            return new RequestExecutor(client, HttpRequestBuilder.get(url), defaultTimeout);
        }

        public RequestExecutor post(String url) {
            return new RequestExecutor(client, HttpRequestBuilder.post(url), defaultTimeout);
        }

        public RequestExecutor put(String url) {
            return new RequestExecutor(client, HttpRequestBuilder.put(url), defaultTimeout);
        }

        public RequestExecutor delete(String url) {
            return new RequestExecutor(client, HttpRequestBuilder.delete(url), defaultTimeout);
        }

        public RequestExecutor patch(String url) {
            return new RequestExecutor(client, HttpRequestBuilder.patch(url), defaultTimeout);
        }

        public Result<HttpResult, WrappedError> send(HttpRequest request) {
            return HttpUtil.send(client, request);
        }

        public CompletableFuture<Result<HttpResult, WrappedError>> sendAsync(HttpRequest request) {
            return HttpUtil.sendAsync(client, request);
        }
    }

    // ==================== 请求执行器 ====================

    /**
     * <b>请求执行器</b>
     * <p>封装请求构建和执行逻辑，提供链式 API</p>
     */
    public static class RequestExecutor {
        private final HttpClient client;
        private final HttpRequestBuilder builder;
        private final Duration defaultTimeout;
        private int maxRetries = 0;
        private Duration retryDelay = Duration.ofMillis(500);

        RequestExecutor(HttpClient client, HttpRequestBuilder builder, Duration defaultTimeout) {
            this.client = client;
            this.builder = builder;
            this.defaultTimeout = defaultTimeout;
        }

        // -------------------- 委托给 Builder --------------------

        public RequestExecutor query(String name, Object value) {
            builder.query(name, value);
            return this;
        }

        public RequestExecutor header(String name, String value) {
            builder.header(name, value);
            return this;
        }

        public RequestExecutor bearerAuth(String token) {
            builder.bearerAuth(token);
            return this;
        }

        public RequestExecutor basicAuth(String username, String password) {
            builder.basicAuth(username, password);
            return this;
        }

        public RequestExecutor accept(String mediaType) {
            builder.accept(mediaType);
            return this;
        }

        public RequestExecutor json(Object body) {
            builder.json(body);
            return this;
        }

        public RequestExecutor form(String name, Object value) {
            builder.form(name, value);
            return this;
        }

        public RequestExecutor text(String text) {
            builder.text(text);
            return this;
        }

        public RequestExecutor bytes(byte[] bytes) {
            builder.bytes(bytes);
            return this;
        }

        public RequestExecutor timeout(Duration timeout) {
            builder.timeout(timeout);
            return this;
        }

        // -------------------- 重试配置 --------------------

        /**
         * <b>设置重试次数</b>
         *
         * @param maxRetries 最大重试次数
         *
         * @return this
         */
        public RequestExecutor retry(int maxRetries) {
            this.maxRetries = Math.max(0, maxRetries);
            return this;
        }

        /**
         * <b>设置重试配置</b>
         *
         * @param maxRetries 最大重试次数
         * @param delay      重试间隔
         *
         * @return this
         */
        public RequestExecutor retry(int maxRetries, Duration delay) {
            this.maxRetries = Math.max(0, maxRetries);
            this.retryDelay = delay;
            return this;
        }

        // -------------------- 执行方法 --------------------

        /**
         * <b>同步发送请求</b>
         *
         * @return Result 包含 HttpResult 或错误
         */
        public Result<HttpResult, WrappedError> send() {
            // 设置默认超时
            builder.timeout(defaultTimeout);

            return builder.build().flatMap(request -> {
                Result<HttpResult, WrappedError> result = HttpUtil.send(client, request);

                // 重试逻辑
                int attempts = 0;
                while (result.isErr() && attempts < maxRetries) {
                    attempts++;
                    LogUtil.warn("HTTP request failed, retrying ({}/{})...", attempts, maxRetries);
                    try {
                        Thread.sleep(retryDelay.toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    result = HttpUtil.send(client, request);
                }

                return result;
            });
        }

        /**
         * <b>异步发送请求</b>
         *
         * @return CompletableFuture
         */
        public CompletableFuture<Result<HttpResult, WrappedError>> sendAsync() {
            builder.timeout(defaultTimeout);

            return builder.build()
                    .map(request -> HttpUtil.sendAsync(client, request))
                    .orElseGet(() -> CompletableFuture.completedFuture(
                            Result.err(WrappedError.of(FacilityErrorType.BUILD_HTTP_REQUEST_BODY_ERROR))
                    ));
        }

        /**
         * <b>发送并解析为指定类型</b>
         *
         * @param clazz 目标类型
         * @param <T>   类型泛型
         *
         * @return Result 包含解析结果或错误
         */
        public <T> Result<T, WrappedError> sendAndParse(Class<T> clazz) {
            return send()
                    .flatMap(HttpResult::requireSuccess)
                    .flatMap(r -> r.json(clazz));
        }

        /**
         * <b>发送并处理响应</b>
         *
         * @param handler 响应处理器
         * @param <T>     结果类型
         *
         * @return Result 包含处理结果或错误
         */
        public <T> Result<T, WrappedError> sendAndHandle(Function<HttpResult, Result<T, WrappedError>> handler) {
            return send().flatMap(handler);
        }
    }
}
