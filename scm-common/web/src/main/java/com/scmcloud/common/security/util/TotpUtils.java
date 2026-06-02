package com.scmcloud.common.security.util;

import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
/**
 * TOTP 双因素认证工具类
 *
 * @author Deng
 * createData 2025/11/5 17:20
 * @version 1.0
 */
@Component
@Slf4j
public class TotpUtils {
    private static final int SECRET_SIZE = 20; // 160 bits
    private static final String ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP = 30; // 30秒时间窗�
    private static final int DIGITS = 6; // 6位验证码
    private static final int WINDOW = 1; // 允许前后1个时间窗口（防止时间误差�

    /**
     * 生成密钥（Base32编码�
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        return BaseEncoding.base32().encode(bytes);
    }

    /**
     * 生成二维码URL（用于Google Authenticator扫描�
     *
     * @param account 账户名（如邮箱或用户名）
     * @param issuer 发行者（应用名称�
     * @param secret Base32编码的密�
     */
    public String generateQrCodeUrl(String account, String issuer, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, account, secret, issuer, DIGITS, TIME_STEP
        );
    }

    /**
     * 验证 TOTP验证�
     *
     * @param secret Base32编码的密�
     * @param code 用户输入�位验证码
     * @return 验证是否通过
     */
    public boolean verifyCode(String secret, String code) {
        if (code == null || code.length() != DIGITS) {
            return false;
        }

        try {
            long currentTime = Instant.now().getEpochSecond() / TIME_STEP;

            // 检查当前时间窗口和前后�WINDOW个窗�
            for (int i = -WINDOW; i <= WINDOW; i++) {
                long time = currentTime + i;
                String generatedCode = generateCode(secret, time);

                if (code.equals(generatedCode)) {
                    log.debug("TOTP验证成功，时间偏� {}", i);
                    return true;
                }
            }

            log.warn("TOTP 验证失败");
            return false;
        } catch (Exception e) {
            log.error("TOTP 验证异常", e);
            return false;
        }
    }

    /**
     * 生成指定时间的验证码
     */
    private String generateCode(String secret, long timeCounter)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = BaseEncoding.base32().decode(secret);

        // 时间转换�字节数组
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(timeCounter);
        byte[] timeBytes = buffer.array();

        // HMAC-SHA1
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key, ALGORITHM));
        byte[] hash = mac.doFinal(timeBytes);

        // 动态截�
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, DIGITS);

        // 格式化为6位字符串（前导零补齐�
        return String.format("%0" + DIGITS + "d", otp);
    }

    /**
     * 获取当前验证码（用于测试�
     */
    public String getCurrentCode(String secret) {
        try {
            long currentTime = Instant.now().getEpochSecond() / TIME_STEP;
            return generateCode(secret, currentTime);
        } catch (Exception e) {
            log.error("生成验证码失败", e);
            return null;
        }
    }
}
