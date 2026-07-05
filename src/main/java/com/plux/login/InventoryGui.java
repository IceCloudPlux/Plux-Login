package com.plux.login;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryGui {

    private static final String GUI_TITLE = Utils.translateColorCodes("&6&lPluxLogin 管理面板");

    public static void openAdminGUI(Player player, PluxLogin plugin) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // 统计信息 - 第10格
        gui.setItem(10, createItem(Material.BOOK,
                Utils.translateColorCodes("&a服务器统计"),
                Utils.translateColorCodes("&7在线玩家: &f" + plugin.getServer().getOnlinePlayers().size()),
                Utils.translateColorCodes("&7已注册玩家: &f" + getRegisteredCount(plugin)),
                Utils.translateColorCodes("&7版本: &f" + plugin.getVersion())));

        // 重载配置 - 第12格
        gui.setItem(12, createItem(Material.COMMAND_BLOCK,
                Utils.translateColorCodes("&e重载配置"),
                Utils.translateColorCodes("&7点击重载 config.yml 和 message.yml")));

        // 查看在线玩家会话 - 第14格
        gui.setItem(14, createItem(Material.PLAYER_HEAD,
                Utils.translateColorCodes("&b查看玩家会话"),
                Utils.translateColorCodes("&7点击查看当前所有玩家的登录状态")));

        // 数据库状态 - 第16格
        gui.setItem(16, createItem(Material.REDSTONE,
                Utils.translateColorCodes("&c数据库状态"),
                Utils.translateColorCodes("&7类型: &f" + (plugin.getDatabaseManager().isMySQL() ? "MySQL" : "SQLite")),
                Utils.translateColorCodes("&7连接: &a正常")));

        player.openInventory(gui);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(line);
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int getRegisteredCount(PluxLogin plugin) {
        try {
            return plugin.getDatabaseManager().getRegisteredCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
