package com.plux.login;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final String qq;
    private final String totpSecret;
    private final long lastLogin;
    private final long registerTime;

    public PlayerData(UUID uuid, String username, String passwordHash, String email, String qq, String totpSecret, long lastLogin, long registerTime) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.qq = qq;
        this.totpSecret = totpSecret;
        this.lastLogin = lastLogin;
        this.registerTime = registerTime;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public String getEmail() {
        return this.email;
    }

    public String getQq() {
        return this.qq;
    }

    public String getTotpSecret() {
        return this.totpSecret;
    }

    public boolean is2FAEnabled() {
        return this.totpSecret != null && !this.totpSecret.isEmpty();
    }

    public long getLastLogin() {
        return this.lastLogin;
    }

    public long getRegisterTime() {
        return this.registerTime;
    }
}