package com.frog.common.rest.interceptor;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.frog.common.util.UUIDv7Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RestClient 请求签名拦截器
 * <p>替代 OpenFeign 的 FeignRequestSignatureInterceptor</p>
 *
 * <p>功能：
 * <ul>
 *   <li>自动为所有 HTTP 请求添加 HMAC-SHA256 签名</li>
 *   <li>防重放攻击：使用时间戳 + nonce</li>
 *   <li>参数排序：确保签名一致性</li>
 *   <li>服务间认证：基于 App-ID 和 Secret-Key</li>
 * </ul>
 *
 * <p>签名格式：
 * <pre>
 * signature = HMAC-SHA256(secretKey, timestamp + nonce + appId + uri + sortedParams)
 * </pre>
 *
 * <p>HTTP Headers：
 * <pre>
 * X-Timestamp: 1640995200000
 * X-Nonce: 550e8400e29b41d4a716446655440000
 * X-Signature: a1b2c3d4e5f6...
 * X-App-Id: internal-service
 * </pre>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Component
@Slf4j
public class RestClientRequestSignatureInterceptor implements ClientHttpRequestInterceptor {

    @Value("${security.feign.app-id:${API_SECRET_INTERNAL_SERVICE:internal-service}}")
    private String appId;

    @Value("${security.feign.secret-key:${API_SECRET_INTERNAL_SECRET:your-internal-secret-key}}")
    private String secretKey;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        // 生成时间戳和 nonce
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUIDv7Util.generate().toString().replace("-", "");

        // 计算签名
        String signature = calculateSignature(request, timestamp, nonce);

        // 添加签名 Header
        request.getHeaders().set("X-Timestamp", timestamp);
        request.getHeaders().set("X-Nonce", nonce);
        request.getHeaders().set("X-Signature", signature);
        request.getHeaders().set("X-App-Id", appId);

        if (log.isDebugEnabled()) {
            log.debug("RestClient request signed: {} {}, signature: {}",
                      request.getMethod(), request.getURI(), signature);
        }

        // 继续执行请求
        return execution.execute(request, body);
    }

    /**
     * 计算请求签名
     *
     * <p>签名算法：
     * <pre>
     * 1. 提取 URI 路径（不含域名和端口）
     * 2. 提取查询参数并按 key 排序
     * 3. 拼接签名内容：timestamp + nonce + appId + uri + sortedParams
     * 4. 使用 HMAC-SHA256 计算签名
     * </pre>
     *
     * @param request   HTTP 请求
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @return 签名字符串（十六进制）
     */
    private String calculateSignature(HttpRequest request, String timestamp, String nonce) {
        // 获取 URI 路径
        String uri = request.getURI().getPath();

        // 获取查询参数并排序
        Map<String, String> params = new HashMap<>();
        String query = request.getURI().getQuery();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                String key = kv[0];
                String value = kv.length > 1 ? kv[1] : "";
                params.put(key, value);
            }
        }

        // 排序并拼接参数
        String sortedParams = sortAndConcatParams(params);

        // 构建签名内容
        String signContent = timestamp + nonce + appId + uri + sortedParams;

        if (log.isTraceEnabled()) {
            log.trace("Signature content: {}", signContent);
        }

        // 计算 HMAC-SHA256 签名
        return SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
            .digestHex(signContent);
    }

    /**
     * 对参数进行排序并拼接
     *
     * <p>格式：key1=value1&key2=value2&...（按 key 字典序排序）</p>
     *
     * @param params 参数 Map
     * @return 排序后的参数字符串
     */
    private String sortAndConcatParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        return params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
    }
}
