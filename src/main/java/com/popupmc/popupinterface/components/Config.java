package com.popupmc.popupinterface.components;
import com.popupmc.popupinterface.PopupInterface;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    public Config(PopupInterface plugin) {
        this.plugin = plugin;

        // Create if doesnt exist
        plugin.saveDefaultConfig();
        load();
    }

    public boolean load() {
        // Load main config file
        FileConfiguration configFile = plugin.getConfig();

        socketHost = configFile.getString("web-socket.host", "localhost");
        socketPort = configFile.getInt("web-socket.port", 80);
        discordBotToken = configFile.getString("discord-token", discordBotTokenDefault);

        return true;
    }

    public PopupInterface plugin;

    public String socketHost;
    public int socketPort;

    public String discordBotToken;
    public static final String discordBotTokenDefault = "Enter token here";
}
