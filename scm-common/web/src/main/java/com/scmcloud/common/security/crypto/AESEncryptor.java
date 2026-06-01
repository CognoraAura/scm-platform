package com.scmcloud.common.security.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;

/**
 * AES 加密工具�?
 *
 * @author Deng
 * createData 2025/10/24 15:06
 * @version 1.0
 */
@Component
@Slf4j
public class AESEncryptor {

    @Value("${security.crypto.aes-key}")
    private String aesKey;

    private AES aes;

    @PostConstruct
    public void init() {
        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
        this.aes = SecureUtil.aes(key);
    }

    /**
     * 加密
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            return aes.encryptBase64(plainText);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("数据加密失败", e);
        }
    }

    /**
     * 解密
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            return aes.decryptStr(cipherText);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("数据解密失败", e);
        }
    }
}
