package com.scoder.jusic.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WeapiCrypto {

    private static final String IV = "0102030405060708";
    private static final String PRESET_KEY = "0CoJUm6Qyw8W8jud";
    private static final String PUBLIC_KEY = "010001";
    private static final String MODULUS =
            "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";

    private static String aesEncrypt(String text, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    private static String rsaEncrypt(String text) {
        StringBuilder reversed = new StringBuilder(text).reverse();
        BigInteger biText = new BigInteger(1, reversed.toString().getBytes(StandardCharsets.UTF_8));
        BigInteger biEx = new BigInteger(PUBLIC_KEY, 16);
        BigInteger biMod = new BigInteger(MODULUS, 16);
        BigInteger biResult = biText.modPow(biEx, biMod);
        return biResult.toString(16).toLowerCase();
    }

    private static String createSecretKey(int size) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static Map<String, String> encrypt(String jsonText) {
        String secretKey = createSecretKey(16);
        String params = aesEncrypt(aesEncrypt(jsonText, PRESET_KEY), secretKey);
        String encSecKey = rsaEncrypt(secretKey);
        Map<String, String> result = new HashMap<>();
        result.put("params", params);
        result.put("encSecKey", encSecKey);
        return result;
    }
}
