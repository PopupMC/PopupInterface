package com.popupmc.popupinterface.discord;

import com.popupmc.popupinterface.socket.LoginAcceptCB;
import com.popupmc.popupinterface.socket.LoginRejectCB;
import org.bukkit.OfflinePlayer;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class LoginRequest {
    public LoginRequest(OfflinePlayer player,
            UUID minecraftUUID,
            String discordUUID,
            WebSocket conn,
            String minecraftUsername,
            LoginAcceptCB loginAcceptCB,
            LoginRejectCB loginRejectCB) {
        this.player = player;
        this.minecraftUUID = minecraftUUID;
        this.discordUUID = discordUUID;
        this.conn = conn;
        this.minecraftUsername = minecraftUsername;
        this.loginAcceptCB = loginAcceptCB;
        this.loginRejectCB = loginRejectCB;

        setExpiration();
    }

    public void setExpiration() {
        // Get UTC Time & add 5 minutes
        LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.UTC);
        dateTime = dateTime.plusMinutes(5);

        // Set expiration
        expiration = dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    // Returns whether expired or not
    public boolean hasExpired() {
        long dateTimeNow = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
        return dateTimeNow >= expiration;
    }

    public OfflinePlayer player;
    public UUID minecraftUUID;
    public String discordUUID;
    public WebSocket conn;
    public String minecraftUsername;
    public LoginAcceptCB loginAcceptCB;
    public LoginRejectCB loginRejectCB;
    public long expiration;
}
