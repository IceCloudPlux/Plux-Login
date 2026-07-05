package com.plux.login.adapter;

import java.lang.reflect.Method;
import org.bukkit.entity.Player;

/**
 * 1.7.x 版本 NMS 适配器
 *
 * 使用反射工具类（带缓存）实现 Title 和 ActionBar 功能：
 * - Title: 1.7.x 无原生 API，降级为聊天消息
 * - ActionBar: 通过 PacketPlayOutChat(byte=2) 发送
 */
public class NMSAdapter_v1_7_R4 implements NMSAdapter {

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // 1.7.x 无原生 Title API，降级为聊天消息
        if (title != null && !title.isEmpty()) {
            player.sendMessage(title);
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendMessage(subtitle);
        }
    }

    @Override
    public void sendActionBar(Player player, String message) {
        try {
            // 使用缓存反射工具获取对象链: Player -> EntityPlayer -> PlayerConnection
            Object entityPlayer = ReflectionUtil.getEntityPlayer(player);
            if (entityPlayer == null) {
                player.sendMessage(message);
                return;
            }

            Object playerConnection = ReflectionUtil.getPlayerConnection(entityPlayer);
            if (playerConnection == null) {
                player.sendMessage(message);
                return;
            }

            // 构造 ChatComponentText 对象（缓存构造器）
            Class<?> chatComponentTextClass = ReflectionUtil.getNmsClass("ChatComponentText");
            Object chatComponent = ReflectionUtil.newInstance(
                    ReflectionUtil.getConstructor(chatComponentTextClass, String.class),
                    message);

            if (chatComponent == null) {
                player.sendMessage(message);
                return;
            }

            // 构造 PacketPlayOutChat 对象（type=2 表示 ACTION_BAR）
            Class<?> packetPlayOutChatClass = ReflectionUtil.getNmsClass("PacketPlayOutChat");
            Object packet = ReflectionUtil.newInstance(
                    ReflectionUtil.getConstructor(packetPlayOutChatClass,
                            ReflectionUtil.getNmsClass("IChatBaseComponent"), byte.class),
                    chatComponent, (byte) 2);

            if (packet == null) {
                player.sendMessage(message);
                return;
            }

            // 发送数据包（缓存 sendPacket 方法）
            Method sendPacketMethod = ReflectionUtil.getMethod(
                    playerConnection.getClass(), "sendPacket",
                    ReflectionUtil.getNmsClass("Packet"));
            ReflectionUtil.invoke(sendPacketMethod, playerConnection, packet);

        } catch (Exception e) {
            // 所有异常降级为普通消息，避免崩溃
            player.sendMessage(message);
        }
    }
}
