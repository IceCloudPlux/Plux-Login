package com.plux.login;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class WebApiServer {

    private final PluxLogin plugin;
    private HttpServer server;
    private boolean running = false;
    private long startTime;

    public WebApiServer(PluxLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动 Web API 服务器
     */
    public void start() {
        if (!plugin.getConfigManager().isWebApiEnabled()) return;
        this.startTime = System.currentTimeMillis();

        int port = plugin.getConfigManager().getWebApiPort();
        String secret = plugin.getConfigManager().getWebApiSecret();

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/", exchange -> {
                // CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

                // 鉴权检查
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.equals("Bearer " + secret)) {
                    sendJson(exchange, 401, "{\"error\":\"Unauthorized\",\"code\":401}");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String response;

                switch (path) {
                    case "/api/status":
                        response = handleStatus();
                        break;
                    case "/api/players":
                        response = handlePlayers();
                        break;
                    case "/api/player":
                        String name = exchange.getRequestURI().getQuery();
                        if (name != null && name.startsWith("name=")) {
                            response = handlePlayerInfo(name.substring(5));
                        } else {
                            response = "{\"error\":\"Missing parameter: name\"}";
                        }
                        break;
                    default:
                        response = "{\"error\":\"Not Found\",\"code\":404}";
                        exchange.sendResponseHeaders(404, 0);
                        break;
                }
                sendJson(exchange, 200, response);
            });

            server.setExecutor(null); // 使用默认执行器
            server.start();
            running = true;
            plugin.getLogger().info("[PluxLogin] Web API 已启动，端口: " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("[PluxLogin] Web API 启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止 Web API 服务器
     */
    public void stop() {
        if (server != null && running) {
            server.stop(0);
            running = false;
            plugin.getLogger().info("[PluxLogin] Web API 已停止");
        }
    }

    private void sendJson(com.sun.net.httpserver.HttpExchange exchange, int code, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException ignored) {}
    }

    // ========== API 处理方法 ==========

    /** 服务器状态接口 */
    private String handleStatus() {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        int registered = 0;
        try { registered = plugin.getDatabaseManager().getRegisteredCount(); } catch (Exception ignored) {}

        return "{"
                + "\"plugin\":\"PluxLogin\","
                + "\"version\":\"" + plugin.getVersion() + "\","
                + "\"online\":" + online + ","
                + "\"maxPlayers\":" + max + ","
                + "\"registered\":" + registered + ","
                + "\"database\":\"" + (plugin.getDatabaseManager().isMySQL() ? "mysql" : "sqlite") + "\","
                + "\"uptime\":" + (System.currentTimeMillis() - getPluginStartTime()) / 1000
                + "}";
    }

    /** 在线玩家列表接口 */
    private String handlePlayers() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            PluxLogin.PlayerSession session = plugin.getPlayerSession(p.getUniqueId());
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
                    .append("\"name\":\"").append(p.getName()).append("\",")
                    .append("\"uuid\":\"").append(p.getUniqueId().toString()).append("\",")
                    .append("\"loggedIn\":").append(session != null && session.isLoggedIn())
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /** 玩家信息查询接口 */
    private String handlePlayerInfo(String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return "{\"error\":\"Player not found\"}";
        }

        UUID uuid = offlinePlayer.getUniqueId();
        PlayerData data = null;
        try { data = plugin.getDatabaseManager().getPlayerData(uuid); } catch (Exception ignored) {}

        if (data == null) {
            return "{\"error\":\"Player data not found in database\"}";
        }

        return "{"
                + "\"name\":\"" + playerName + "\","
                + "\"uuid\":\"" + uuid.toString() + "\","
                + "\"registered\":" + (data.getPasswordHash() != null && !data.getPasswordHash().isEmpty()) + ","
                + "\"email\":\"" + (data.getEmail() != null ? data.getEmail() : "") + "\","
                + "\"qq\":\"" + (data.getQq() != null ? data.getQq() : "") + "\","
                + "\"2FAEnabled\":" + data.is2FAEnabled() + ","
                + "\"isOnline\":" + offlinePlayer.isOnline()
                + "}";
    }

    public boolean isRunning() { return running; }

    private long getPluginStartTime() { return startTime; }
}
