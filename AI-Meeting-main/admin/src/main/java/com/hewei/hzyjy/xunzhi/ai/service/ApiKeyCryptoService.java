package com.hewei.hzyjy.xunzhi.ai.service;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户 API Key 加解密服务（AES-GCM）
 * 密文格式：enc:v1: + Base64(iv + ciphertext)；无前缀的存量明文原样返回，零迁移兼容
 */
@Component
public class ApiKeyCryptoService {

    private static final String PREFIX = "enc:v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Base64 编码的 AES 密钥（16/24/32 字节），来自环境变量 BYOK_ENCRYPT_KEY
     */
    @Value("${byok.encrypt-key:}")
    private String encryptKeyBase64;

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }
        byte[] key = requireKey();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new ClientException("API Key 加密失败");
        }
    }

    public String decryptIfNeeded(String value) {
        if (!isEncrypted(value)) {
            return value;
        }
        byte[] key = requireKey();
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, payload, 0, GCM_IV_LENGTH));
            byte[] plain = cipher.doFinal(payload, GCM_IV_LENGTH, payload.length - GCM_IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ClientException("API Key 解密失败，请检查 BYOK_ENCRYPT_KEY 配置是否变更");
        }
    }

    private byte[] requireKey() {
        if (StrUtil.isBlank(encryptKeyBase64)) {
            throw new ClientException("服务端未配置 BYOK_ENCRYPT_KEY，无法保存或使用用户 API Key");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encryptKeyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new ClientException("BYOK_ENCRYPT_KEY 不是合法的 Base64 字符串");
        }
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new ClientException("BYOK_ENCRYPT_KEY 解码后长度必须为 16/24/32 字节");
        }
        return key;
    }
}
