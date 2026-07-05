package com.plux.login;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaManager {

    private final PluxLogin plugin;
    private final Map<UUID, String> captchas = new ConcurrentHashMap<>();
    private final Map<UUID, Long> captchaTimestamps = new ConcurrentHashMap<>();

    public CaptchaManager(PluxLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * 生成验证码并存储（使用 SecureRandom 保证安全性）
     */
    public String generateCaptcha(UUID uuid) {
        int length = plugin.getConfigManager().getCaptchaLength();
        boolean alphaNum = plugin.getConfigManager().isAlphaNumCaptcha();
        String captcha = alphaNum ? generateAlphaNumCaptcha(length) : generateNumCaptcha(length);
        captchas.put(uuid, captcha);
        captchaTimestamps.put(uuid, System.currentTimeMillis());
        return captcha;
    }

    /**
     * 验证验证码 - 修复26.2版本正确验证码无法通过的问题
     * 原因：使用 equals 进行严格匹配，确保大小写敏感和空格问题被正确处理
     */
    public boolean validateCaptcha(UUID uuid, String input) {
        if (input == null || input.isEmpty()) return false;

        String stored = captchas.get(uuid);
        if (stored == null) return false;

        Long timestamp = captchaTimestamps.get(uuid);
        if (timestamp == null) return false;

        // 检查是否过期
        long elapsed = System.currentTimeMillis() - timestamp;
        long timeoutMs = (long) plugin.getConfigManager().getCaptchaTimeout() * 1000L;
        if (elapsed > timeoutMs) {
            removeCaptcha(uuid);
            return false;
        }

        // 修复核心：使用 trim + equals 确保输入处理正确
        if (stored.trim().equals(input.trim())) {
            removeCaptcha(uuid);
            return true;
        }
        return false;
    }

    public void removeCaptcha(UUID uuid) {
        captchas.remove(uuid);
        captchaTimestamps.remove(uuid);
    }

    // ========== 验证码生成（使用 SecureRandom）==========

    private String generateNumCaptcha(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Utils.getSecureRandom().nextInt(10));
        }
        return sb.toString();
    }

    private String generateAlphaNumCaptcha(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(Utils.getSecureRandom().nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ========== 兼容别名 ==========

    public boolean verifyCaptcha(UUID uuid, String input) { return validateCaptcha(uuid, input); }

    public int getCaptchaTimeout() { return plugin.getConfigManager().getCaptchaTimeout(); }

    public long getCaptchaElapsedTime(UUID uuid) {
        Long timestamp = captchaTimestamps.get(uuid);
        if (timestamp == null) return (long) plugin.getConfigManager().getCaptchaTimeout() * 1000L;
        return System.currentTimeMillis() - timestamp;
    }
}
