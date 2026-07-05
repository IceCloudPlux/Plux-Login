package com.plux.login.adapter;

import org.bukkit.Bukkit;

/**
 * 版本工具类
 *
 * 全版本兼容规范（基于工业级插件开发范式）：
 * 1. 禁止使用 Bukkit.getMinecraftVersion()（1.13+ 才加入）
 * 2. NMS 版本通过 Server 类包名解析
 * 3. MC 原版版本通过 getVersion() 的 (MC: ) 标记解析
 */
public final class VersionUtil {

    private static String nmsVersion;
    private static String mcVersion;
    private static int majorVersion = 1;
    private static int minorVersion = 8;
    private static boolean initialized = false;

    private VersionUtil() {}

    /**
     * 初始化版本信息（线程安全，仅执行一次）
     */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // 1. 获取 NMS 版本（全版本通用，返回如 v1_26_R1）
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            nmsVersion = pkg.substring(pkg.lastIndexOf(".") + 1);
        } catch (Exception e) {
            nmsVersion = "";
            Bukkit.getLogger().warning("[PluxLogin] 无法获取 NMS 版本: " + e.getMessage());
        }

        // 2. 获取 MC 原版版本（返回如 1.26.1、1.20.1、1.7.10）
        // 使用指南推荐的标准方式：通过 (MC: ) 标记解析
        try {
            String ver = Bukkit.getVersion();
            int start = ver.indexOf("(MC: ");
            if (start >= 0) {
                start += 5; // 跳过 "(MC: "
                int end = ver.indexOf(')', start);
                mcVersion = (end > start) ? ver.substring(start, end) : ver;
            } else {
                // 降级方案：使用 bukkitVersion
                mcVersion = Bukkit.getBukkitVersion().split("-")[0];
            }
        } catch (Exception e) {
            mcVersion = "1.8.8";
        }

        // 3. 解析主次版本号
        parseVersionNumbers(mcVersion);

        Bukkit.getLogger().info("[PluxLogin] 版本检测完成 -> NMS: " + nmsVersion + ", MC: " + mcVersion);
    }

    /**
     * 获取 NMS 包名版本（如 v1_26_R1、v1_7_R4）
     * 全版本通用，兼容 1.7.10 ~ 26.1+
     */
    public static String getNMSVersion() {
        if (!initialized) init();
        return nmsVersion;
    }

    /**
     * 获取 Minecraft 原版版本号（如 1.26.1、1.7.10）
     * 格式统一为 x.y.z
     */
    public static String getMcVersion() {
        if (!initialized) init();
        return mcVersion;
    }

    /**
     * 获取主版本号（如 1.26 中的 26）
     */
    public static int getMajorVersion() {
        if (!initialized) init();
        return majorVersion;
    }

    /**
     * 获取次版本号
     */
    public static int getMinorVersion() {
        if (!initialized) init();
        return minorVersion;
    }

    // ========== 版本判断辅助方法 ==========

    /** 是否为 1.7.x 版本（需要特殊适配） */
    public static boolean isBelow_1_8() {
        if (!initialized) init();
        return majorVersion == 1 && minorVersion < 8;
    }

    /** 是否为 1.12 及以下版本（ID系统变更分界线） */
    public static boolean isBelowOrEqual_1_12() {
        if (!initialized) init();
        return majorVersion == 1 && minorVersion <= 12;
    }

    /** 是否为 1.16.5 及以下版本（Java 分界线） */
    public static boolean isBelowOrEqual_1_16() {
        if (!initialized) init();
        return majorVersion == 1 && minorVersion <= 16;
    }

    /** 是否为 1.20.4 及以下版本 */
    public static boolean isBelowOrEqual_1_20_4() {
        if (!initialized) init();
        if (majorVersion > 1) return false;
        if (minorVersion < 20) return true;
        // 检查补丁版本
        String[] parts = mcVersion.split("\\.");
        return parts.length >= 3 && Integer.parseInt(parts[2]) <= 4;
    }

    /** 是否为 1.25.x 或更高版本 */
    public static boolean isAtLeast_1_25() {
        if (!initialized) init();
        return majorVersion > 1 || (majorVersion == 1 && minorVersion >= 25);
    }

    /** 是否为 26.1+ 版本（官方新命名规则） */
    public static boolean isAtLeast_26_1() {
        if (!initialized) init();
        // 26.1 对应的社区格式为 1.26.1
        return majorVersion > 1 || (majorVersion == 1 && minorVersion >= 26);
    }

    // ========== 内部方法 ==========

    private static void parseVersionNumbers(String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                majorVersion = Integer.parseInt(parts[0]);
                minorVersion = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            // 解析失败使用默认值
            majorVersion = 1;
            minorVersion = 8;
        }
    }
}
