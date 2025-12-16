package com.github.ryan.facility.http.client.model;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * <b>HTTP 客户端配置</b>
 * <p>
 * 用于配置 HTTP 客户端的行为，支持链式构建。
 * 配置完成后可通过 {@link #build()} 创建 {@link HttpClient} 实例。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * HttpClient client = HttpClientConfig.create()
 *     .version(HttpClient.Version.HTTP_2)
 *     .connectTimeout(Duration.ofSeconds(30))
 *     .proxy("proxy.example.com", 8080)
 *     .followRedirects(true)
 *     .build();
 * }</pre>
 *
 * @author yvvb
 * @since 2025/5/4
 */
public final class HttpClientConfig {

    // ==================== 默认值 ====================

    /**
     * 默认连接超时时间
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 默认请求超时时间
     */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    // ==================== 配置字段 ====================

    private HttpClient.Version version = HttpClient.Version.HTTP_1_1;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    private HttpClient.Redirect redirectPolicy = HttpClient.Redirect.NORMAL;
    private ProxySelector proxySelector;
    private Authenticator authenticator;
    private Executor executor;

    private HttpClientConfig() {
    }

    // ==================== 工厂方法 ====================

    /**
     * <b>创建新的配置实例</b>
     *
     * @return HttpClientConfig 实例
     */
    public static HttpClientConfig create() {
        return new HttpClientConfig();
    }

    /**
     * <b>创建默认配置的 HttpClient</b>
     *
     * @return HttpClient 实例
     */
    public static HttpClient defaultClient() {
        return create().build();
    }

    // ==================== 配置方法 ====================

    /**
     * <b>设置 HTTP 协议版本</b>
     *
     * @param version HTTP/1.1 或 HTTP/2
     * @return this
     */
    public HttpClientConfig version(HttpClient.Version version) {
        this.version = Objects.requireNonNull(version);
        return this;
    }

    /**
     * <b>设置连接超时时间</b>
     *
     * @param timeout 超时时间
     * @return this
     */
    public HttpClientConfig connectTimeout(Duration timeout) {
        this.connectTimeout = Objects.requireNonNull(timeout);
        return this;
    }

    /**
     * <b>设置请求超时时间</b>
     *
     * @param timeout 超时时间
     * @return this
     */
    public HttpClientConfig requestTimeout(Duration timeout) {
        this.requestTimeout = Objects.requireNonNull(timeout);
        return this;
    }

    /**
     * <b>设置代理服务器</b>
     *
     * @param host 代理主机
     * @param port 代理端口
     * @return this
     */
    public HttpClientConfig proxy(String host, int port) {
        if (host != null && !host.isBlank() && port > 0) {
            this.proxySelector = ProxySelector.of(new InetSocketAddress(host, port));
        }
        return this;
    }

    /**
     * <b>设置代理选择器</b>
     *
     * @param proxySelector 代理选择器
     * @return this
     */
    public HttpClientConfig proxy(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
        return this;
    }

    /**
     * <b>设置是否跟随重定向</b>
     *
     * @param follow true-跟随重定向，false-不跟随
     * @return this
     */
    public HttpClientConfig followRedirects(boolean follow) {
        this.redirectPolicy = follow ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER;
        return this;
    }

    /**
     * <b>设置重定向策略</b>
     *
     * @param policy 重定向策略
     * @return this
     */
    public HttpClientConfig redirectPolicy(HttpClient.Redirect policy) {
        this.redirectPolicy = Objects.requireNonNull(policy);
        return this;
    }

    /**
     * <b>设置认证器</b>
     *
     * @param authenticator 认证器
     * @return this
     */
    public HttpClientConfig authenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
        return this;
    }

    /**
     * <b>设置执行器（用于异步请求）</b>
     *
     * @param executor 线程池
     * @return this
     */
    public HttpClientConfig executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    // ==================== 构建方法 ====================

    /**
     * <b>构建 HttpClient 实例</b>
     *
     * @return 配置好的 HttpClient
     */
    public HttpClient build() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(version)
                .connectTimeout(connectTimeout)
                .followRedirects(redirectPolicy);

        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        if (authenticator != null) {
            builder.authenticator(authenticator);
        }
        if (executor != null) {
            builder.executor(executor);
        }

        return builder.build();
    }

    // ==================== Getter ====================

    public Duration getRequestTimeout() {
        return requestTimeout;
    }
}
