package com.scmcloud.common.security.util;

import com.scmcloud.common.security.properties.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一处理 Token提取和设备ID生成
 *
 * @author Deng
 * createData 2025/10/20 16:23
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class HttpServletRequestUtils {
    private final JwtProperties jwtProperties;

    // 设备ID相关常量
    private static final String DEVICE_ID_HEADER = "X-Device-ID";
    private static final int MAX_DEVICE_ID_LENGTH = 128;
    private static final String DEVICE_ID_PATTERN = "[^a-zA-Z0-9-_]";
    private static final String DEFAULT_USER_AGENT = "unknown";
    private static final String DEFAULT_IP = "0.0.0.0";

    // 请求级缓存key
    private static final String CACHED_DEVICE_ID_ATTR = "cached.device.id";

    /**
     * 从HTTP请求中提取JWT token
     *
     * @param request HTTP请求对象
     * @return JWT token，如果不存在或格式错误则返回null
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        try {
            // 获取配置的header名称和prefix
            String headerName = jwtProperties.getHeader();
            String prefix = jwtProperties.getPrefix();

            // 验证配置是否有效
            if (headerName == null || prefix == null) {
                return null;
            }

            // 获取header值并trim
            String bearerToken = request.getHeader(headerName);
            if (!StringUtils.hasText(bearerToken)) {
                return null;
            }

            bearerToken = bearerToken.trim();

            // 验证prefix并提取token
            if (bearerToken.startsWith(prefix)) {
                // 验证长度是否足够
                if (bearerToken.length() <= prefix.length()) {
                    return null;
                }

                String token = bearerToken.substring(prefix.length()).trim();
                return StringUtils.hasText(token) ? token : null;
            }

            return null;
        } catch (Exception e) {
            // 记录异常但不向上抛出，避免影响认证流�?
            return null;
        }
    }

    /**
     * 获取或生成设备ID
     * <p>
     * 优先使用客户端提供的X-Device-ID header，如果不存在则基于User-Agent和IP生成
     * 使用请求级缓存，避免在同一请求中重复计�?
     * </p>
     *
     * @param request HTTP请求对象
     * @return 设备ID，保证非空且长度不超�?28字符
     */
    public String getDeviceId(HttpServletRequest request) {
        // 检查请求级缓存
        Object cached = request.getAttribute(CACHED_DEVICE_ID_ATTR);
        if (cached instanceof String) {
            return (String) cached;
        }

        String deviceId = request.getHeader(DEVICE_ID_HEADER);

        if (StringUtils.hasText(deviceId)) {
            // 清理非法字符（只保留字母、数字、横线、下划线�?
            deviceId = deviceId.replaceAll(DEVICE_ID_PATTERN, "");

            // 如果清理后为空，则重新生�?
            if (!StringUtils.hasText(deviceId)) {
                deviceId = generateDeviceId(request);
            } else if (deviceId.length() > MAX_DEVICE_ID_LENGTH) {
                // 限制长度
                deviceId = deviceId.substring(0, MAX_DEVICE_ID_LENGTH);
            }
        } else {
            // 没有提供设备ID，基于请求信息生�?
            deviceId = generateDeviceId(request);
        }

        // 缓存到请求属性中
        request.setAttribute(CACHED_DEVICE_ID_ATTR, deviceId);

        return deviceId;
    }

    /**
     * 基于请求信息生成设备ID
     * <p>
     * 使用User-Agent和IP地址的组合生成SHA256哈希作为设备ID
     * </p>
     *
     * @param request HTTP请求对象
     * @return 生成的设备ID（SHA256哈希值）
     */
    private String generateDeviceId(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = IpUtils.getClientIp(request);

        // 使用默认值处理null情况
        String safeUserAgent = (userAgent != null && !userAgent.isEmpty()) ? userAgent : DEFAULT_USER_AGENT;
        String safeIp = (ip != null && !ip.isEmpty()) ? ip : DEFAULT_IP;

        // 生成唯一标识
        String raw = safeUserAgent + "|" + safeIp;
        return DigestUtils.sha256Hex(raw);
    }
}
