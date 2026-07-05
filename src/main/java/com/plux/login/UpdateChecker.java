package com.plux.login;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateChecker {
    private final PluxLogin plugin;
    private final Pattern versionPattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    public UpdateChecker(PluxLogin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!this.plugin.getConfigManager().isUpdateCheckerEnabled()) {
            return;
        }
        this.checkForUpdates();
        int interval = 72000;
        new BukkitRunnable(){

            public void run() {
                UpdateChecker.this.checkForUpdates();
            }
        }.runTaskTimerAsynchronously((Plugin)this.plugin, (long)interval, (long)interval);
    }

    public void checkForUpdates() {
        try {
            String currentVersion = this.plugin.getVersion();
            String latestVersion = this.fetchLatestVersion();
            if (latestVersion != null && this.isNewerVersion(latestVersion, currentVersion)) {
                this.plugin.getLogger().info("============== PluxLogin ==============");
                this.plugin.getLogger().info("检测到有新版本可用，当前使用版本: " + currentVersion + "，最新版本: " + latestVersion);
                this.plugin.getLogger().info("请前往Minebbs.com/icecloudplux.github.io进行下载");
                this.plugin.getLogger().info("===================================");
                this.sendUpdateNotificationToAdmins(currentVersion, latestVersion);
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u68c0\u67e5\u66f4\u65b0\u65f6\u51fa\u9519: " + e.getMessage());
        }
    }

    public void sendUpdateNotificationToAdmins(String currentVersion, String latestVersion) {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (!player.hasPermission("pluxlogin.admin")) continue;
            player.sendMessage("============== PluxLogin ==============");
            player.sendMessage("§e检测到有新版本可用，当前使用版本: §a" + currentVersion + "§e，最新版本: §a" + latestVersion);
            player.sendMessage("§e请前往Minebbs.com/icecloudplux.github.io进行下载");
            player.sendMessage("===================================");
        }
    }

    public void notifyAdminOnLogin(Player player, String currentVersion, String latestVersion) {
        if (player.hasPermission("pluxlogin.admin") && latestVersion != null && this.isNewerVersion(latestVersion, currentVersion)) {
            player.sendMessage("============== PluxLogin ==============");
            player.sendMessage("§e检测到有新版本可用，当前使用版本: §a" + currentVersion + "§e，最新版本: §a" + latestVersion);
            player.sendMessage("§e请前往Minebbs.com/icecloudplux.github.io进行下载");
            player.sendMessage("===================================");
        }
    }

    public String fetchLatestVersion() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            URL url = new URL("https://gitee.com/cllupdate/pluginupdate/raw/master/lcloginversion.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "text/plain");
            connection.setRequestProperty("Connection", "keep-alive");
            this.plugin.getLogger().info("\u6b63\u5728\u68c0\u67e5\u66f4\u65b0\uff0c\u8bf7\u6c42URL: " + url.toString());
            int responseCode = connection.getResponseCode();
            this.plugin.getLogger().info("\u66f4\u65b0\u68c0\u67e5\u54cd\u5e94\u7801: " + responseCode);
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String version = reader.readLine().trim();
                reader.close();
                this.plugin.getLogger().info("\u83b7\u53d6\u5230\u7684\u7248\u672c\u53f7: " + version);
                if (this.versionPattern.matcher(version).matches()) {
                    return version;
                }
                this.plugin.getLogger().warning("\u7248\u672c\u53f7\u683c\u5f0f\u4e0d\u6b63\u786e: " + version);
            } else {
                this.plugin.getLogger().warning("\u66f4\u65b0\u68c0\u67e5\u5931\u8d25\uff0c\u54cd\u5e94\u7801: " + responseCode);
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u83b7\u53d6\u6700\u65b0\u7248\u672c\u65f6\u51fa\u9519: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        try {
            String[] latestParts = latestVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            int i = 0;
            while (true) {
                if (i >= Math.max(latestParts.length, currentParts.length)) {
                    return false;
                }
                int latest = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (latest > current) {
                    return true;
                }
                if (latest < current) {
                    return false;
                }
                ++i;
            }
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
}