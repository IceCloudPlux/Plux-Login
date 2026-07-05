package com.plux.login.adapter;

import org.bukkit.entity.Player;

public interface NMSAdapter {

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void sendActionBar(Player player, String message);
}