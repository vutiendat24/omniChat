package com.omnichat.integration.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class HmacUtil {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public boolean verifySignature(String payload, String signature, String appSecret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        String expectedSignature = "sha256=" + generateHmacSha256(payload, appSecret);
        return expectedSignature.equals(signature);
    }

    private String generateHmacSha256(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC: " + e.getMessage(), e);
        }
    }
}
