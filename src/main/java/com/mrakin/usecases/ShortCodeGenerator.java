package com.mrakin.usecases;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class ShortCodeGenerator {
    private static final int SHORT_CODE_LENGTH = 8;

    public String generate(String originalUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(originalUrl.getBytes());
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            // Возвращаем первые 8 символов, которые URL-safe
            return encoded.substring(0, Math.min(encoded.length(), SHORT_CODE_LENGTH));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not found", e);
        }
    }
}
