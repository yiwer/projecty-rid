package com.github.ryan.facility.json.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * <b>InputStream反序列化器</b>
 * <p>
 * 将Base64编码的字符串反序列化为InputStream。
 * 与{@link InputStreamSerializer}配对使用。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * public class FileData {
 *     @JsonDeserialize(using = InputStreamDeserializer.class)
 *     private InputStream content;
 * }
 * }</pre>
 *
 * @author yvvb
 * @since 2025/5/4
 * @see InputStreamSerializer
 */
public class InputStreamDeserializer extends JsonDeserializer<InputStream> {

    /**
     * <b>将Base64字符串反序列化为InputStream</b>
     *
     * @param p    JSON解析器
     * @param ctxt 反序列化上下文
     * @return 解码后的InputStream
     * @throws IOException IO异常
     */
    @Override
    public InputStream deserialize(JsonParser p, DeserializationContext ctx)
            throws IOException {
        // 从 Base64 字符串重建 InputStream
        String base64 = p.getText();
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new ByteArrayInputStream(bytes);
    }
}