package com.plux.login;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class TOTPManager {
    private static final int TOTP_LENGTH = 6;
    private static final long TIME_STEP = 30000;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    public String generateQRCodeURL(String playerName, String secret, String issuer) {
        String label = playerName;
        String parameters = String.format("secret=%s&issuer=%s", secret, issuer);
        String otpAuth = "otpauth://totp/" + label + "?" + parameters;
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + otpAuth;
    }

    public String generateTOTP(String secret) {
        return generateTOTP(secret, System.currentTimeMillis());
    }

    public String generateTOTP(String secret, long timestamp) {
        long timeCounter = timestamp / TIME_STEP;
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeCounter).array();
        
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
            hmac.init(keySpec);
            byte[] hmacResult = hmac.doFinal(timeBytes);
            
            int offset = hmacResult[hmacResult.length - 1] & 0x0F;
            int binary = ((hmacResult[offset] & 0x7F) << 24) |
                         ((hmacResult[offset + 1] & 0xFF) << 16) |
                         ((hmacResult[offset + 2] & 0xFF) << 8) |
                         (hmacResult[offset + 3] & 0xFF);
            
            String totp = String.valueOf(binary % 1000000);
            while (totp.length() < TOTP_LENGTH) {
                totp = "0" + totp;
            }
            return totp;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }

    public boolean verifyTOTP(String secret, String code) {
        if (code == null || code.length() != TOTP_LENGTH) {
            return false;
        }
        
        String expectedCode = generateTOTP(secret);
        if (expectedCode.equals(code)) {
            return true;
        }
        
        String previousCode = generateTOTP(secret, System.currentTimeMillis() - TIME_STEP);
        String nextCode = generateTOTP(secret, System.currentTimeMillis() + TIME_STEP);
        
        return previousCode.equals(code) || nextCode.equals(code);
    }
}