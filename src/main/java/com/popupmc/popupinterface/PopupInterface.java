package com.popupmc.popupinterface;

import com.google.gson.Gson;
import com.popupmc.popupinterface.auth.AuthSessionManager;
import com.popupmc.popupinterface.components.*;
import com.popupmc.popupinterface.discord.Discord;
import com.popupmc.popupinterface.socket.Socket;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public class PopupInterface extends JavaPlugin {
    @Override
    public void onEnable() {
        config = new Config(this);

        // Cont proceed if config fails security check
        if(!checkConfig()) {
            setEnabled(false);
            return;
        }

        socket = new Socket(new InetSocketAddress(config.socketHost, config.socketPort), this);
        authSessionManager = new AuthSessionManager(this);
        discord = new Discord(this);
    }

    // Make sure both the JWT secret and and Bot Token are setup
    public boolean checkConfig() {
        if (config.discordBotToken.equalsIgnoreCase(Config.discordBotTokenDefault)) {
            getLogger().warning("ERROR: You have not set the bot token. Refusing to load plugin...");
            return false;
        }

        return true;
    }

    public AuthSessionManager authSessionManager;
    public Config config;
    public Discord discord;
    public Gson json = new Gson();
    public Socket socket;
}
