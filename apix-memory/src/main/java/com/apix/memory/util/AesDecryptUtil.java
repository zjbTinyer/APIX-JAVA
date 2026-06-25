package com.apix.memory.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-128-CBC 解密工具 — 对标 Electron 前端的加密方式。
 *
 * 前端加密: crypto.createCipheriv('aes-128-cbc', key, iv)
 * Key: '0123456789abcdef' (16 bytes)
 * IV:  'abcdef9876543210' (16 bytes)
 * 输出: base64 编码的密文
 */
public class AesDecryptUtil {

    private static final String KEY = "0123456789abcdef";
    private static final String IV = "abcdef9876543210";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * 解密前端传入的 AES 加密密码。
     *
     * @param encryptedBase64 前端加密后的 base64 字符串
     * @return 解密后的明文字符串
     */
    public static String decrypt(String encryptedBase64) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(cipherBytes);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * 测试入口。
     */
    public static void main(String[] args) {
        // 前端加密 "test123" 的结果用来验证
        String encrypted = "0f3b0c6a5d8e7f2a1b4c9d8e7f6a5b4c"; // 占位
        System.out.println("Decrypted: " + decrypt(encrypted));
    }
}
