package com.popupmc.popupinterface.socket;

import com.popupmc.popupinterface.PopupInterface;
import com.popupmc.popupinterface.auth.AuthResponse;
import com.popupmc.popupinterface.auth.AuthResponseCodes;
import com.popupmc.popupinterface.auth.AuthSession;
import com.popupmc.popupinterface.discord.LoginRequest;
import github.scarsz.discordsrv.DiscordSRV;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public class SocketMessageProcessor {
    public SocketMessageProcessor(@NotNull PopupInterface plugin) {
        this.plugin = plugin;
    }

    public void processMsg(@NotNull WebSocket conn, @NotNull SocketMessage_Base message) {
        if(message instanceof SocketMessage_v1)
            processMsgV1(conn, (SocketMessage_v1)message);
    }

    public void processMsgV1(@NotNull WebSocket conn, @NotNull SocketMessage_v1 message) {
        if(message.ns.equals("login") && message.value != null)
            processLogin(conn, (String)message.value);
    }

    public void processLogin(@NotNull WebSocket conn, @NotNull String mcUsername) {

        // Run this async
        new BukkitRunnable() {
            @Override
            public void run() {
                // Obsolete but its all we have until 1.16 where they have a better method
                OfflinePlayer player = Bukkit.getOfflinePlayer(mcUsername);

                // Give generic error message if user has never logged in before or is banned
                // Never give detailed error messages in these cases because it can reveal information to hackers
                if(!player.hasPlayedBefore() || player.isBanned()) {
                    SocketMessage_v1 msg = new SocketMessage_v1();
                    msg.ns = "error/username";
                    msg.value = "Invalid username";
                    conn.send(plugin.json.toJson(msg));
                    return;
                }

                // Grab UUID
                UUID uuid = player.getUniqueId();

                // Check to see if player has permission to login
                // I just want to say LuckPerms is the absolute worst, I dont even know if I'm doing this right or not
                // All I want to do is just check if an offline user has a particular permission, this is beyond stupid
                LuckPerms luckPerms = LuckPermsProvider.get();
                UserManager userManager = luckPerms.getUserManager();
                User playerLP = userManager.loadUser(uuid).join();

                Collection<Node> playerLPNodes = playerLP.resolveInheritedNodes(QueryOptions.nonContextual());

                boolean hasLoginPerm = false;
                for(Node node : playerLPNodes) {
                    if(node.getKey().equalsIgnoreCase("interface.login") && node.getValue()) {
                        hasLoginPerm = true;
                        break;
                    }
                }

                if(!hasLoginPerm) {
                    SocketMessage_v1 msg = new SocketMessage_v1();
                    msg.ns = "error/username";
                    msg.value = "Invalid username";
                    conn.send(plugin.json.toJson(msg));
                    return;
                }

                // Look it up in Discords linked accounts
                String discordID = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

                // Make sure is valid, Discord actually doesnt have this documented so making a guess
                // Never give detailed error messages in these cases because it can reveal information to hackers
                if(discordID == null || discordID.isEmpty()) {
                    SocketMessage_v1 msg = new SocketMessage_v1();
                    msg.ns = "error/username";
                    msg.value = "Invalid username";
                    conn.send(plugin.json.toJson(msg));
                    return;
                }

                // Make Discord Request
                plugin.discord.requestLogin(new LoginRequest(
                        player,
                        uuid,
                        discordID,
                        conn,
                        mcUsername,
                        SocketMessageProcessor.this::loginAccepted,
                        SocketMessageProcessor.this::loginRejected
                ));
            }
        }.runTaskLaterAsynchronously(plugin, 1);
    }

    public void loginAccepted(LoginRequest request) {

        // Attempt to create session
        AuthResponse response = plugin.authSessionManager.createSession(request.minecraftUUID, request.discordUUID);

        // Generate error code if there is one
        String errorMsg = "";

        switch(response.code) {
            case SERVER_SESSION_LIMIT:
                errorMsg = "The server has reached maximum session limit";
                break;
            case USER_SESSON_LIMIT:
                errorMsg = "You have reached too many sessions, log out of one or wait for one to expire.";
                break;
        }

        // Send error response if there is one
        if(response.code != AuthResponseCodes.SESSION_CREATED) {
            SocketMessage_v1 msg = new SocketMessage_v1();
            msg.ns = "error/session";
            msg.value = errorMsg;
            request.conn.send(plugin.json.toJson(msg));
            return;
        }

        // Get session
        AuthSession session = plugin.authSessionManager.sessions.getOrDefault((long)response.data, null);

        // Error out if session can't be obtained
        if(session == null) {
            SocketMessage_v1 msg = new SocketMessage_v1();
            msg.ns = "error/session";
            msg.value = "An internal error has occured, the session is not obtainable";
            request.conn.send(plugin.json.toJson(msg));
            return;
        }

        // Create a token
        String token = session.createSessionToken();

        // Send token
        SocketMessage_v1 msg = new SocketMessage_v1();
        msg.ns = "login/token";
        msg.value = token;
        request.conn.send(plugin.json.toJson(msg));
    }

    public void loginRejected(LoginRequest request) {
        SocketMessage_v1 msg = new SocketMessage_v1();
        msg.ns = "error/username";
        msg.value = "Invalid username";
        request.conn.send(plugin.json.toJson(msg));
    }

    PopupInterface plugin;
}
