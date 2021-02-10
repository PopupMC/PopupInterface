package com.popupmc.popupinterface.discord;

import com.popupmc.popupinterface.PopupInterface;
import com.popupmc.popupinterface.auth.AuthSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class Discord extends ListenerAdapter {

    public Discord(PopupInterface plugin) {
        this.plugin = plugin;

        try {
            bot = JDABuilder.createDefault(plugin.config.discordBotToken)
                    .setActivity(Activity.watching("for interface requests"))
                    .addEventListeners(this)
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }

        // Start session cleaner 5 minutes from now and have it run every 5 minutes
        // This is the cheaper alternative to using a Redis DB which is WAYYYYY better
        requestCleaner = new BukkitRunnable() {
            @Override
            public void run() {
                cleanRequests();
            }
        }.runTaskTimerAsynchronously(plugin, 5 * 60 * 20, 5 * 60 * 20);
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {

        // Ignore us and any other bot in the DM
        if(event.getAuthor().isBot())
            return;

        String msg = event.getMessage().getContentRaw();

        if(msg.equalsIgnoreCase("Accept Login")) {
            if(!loginRequests.containsKey(event.getAuthor().getId()))
                return;

            LoginRequest loginRequest = loginRequests.getOrDefault(event.getAuthor().getId(), null);
            if(loginRequest == null)
                return;

            loginRequest.loginAcceptCB.accept(loginRequest);
            loginRequests.remove(event.getAuthor().getId());
        }

        if(msg.equalsIgnoreCase("Deny Login")) {
            if(!loginRequests.containsKey(event.getAuthor().getId()))
                return;

            LoginRequest loginRequest = loginRequests.getOrDefault(event.getAuthor().getId(), null);
            if(loginRequest == null)
                return;

            loginRequest.loginRejectCB.reject(loginRequest);
            loginRequests.remove(event.getAuthor().getId());
        }
    }

    public void requestLogin(LoginRequest request) {

        // Block if there's already a pending request
        if(loginRequests.containsKey(request.discordUUID)) {
            request.loginRejectCB.reject(request);
            return;
        }

        // Get User and make sure valid
        User user = bot.getUserById(request.discordUUID);
        if(user == null) {
            request.loginRejectCB.reject(request);
            return;
        }

        // Add request
        loginRequests.put(request.discordUUID, request);

        // Send user a message to accept or deny
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("Someone online has quested access to login and interface with your account on this " +
                    "server. If you accept please reply with `Accept Login` otherwise you can ignore it and let it expire " +
                    "in 5 minutes or reply with `Deny Login`.");
        });
    }

    public void cleanRequests() {
        for(LoginRequest request : loginRequests.values()) {
            if(request.hasExpired())
                request.loginRejectCB.reject(request);
                loginRequests.remove(request.discordUUID);
        }
    }

    // Discord Bot
    public JDA bot;

    // Pending login requests
    public HashMap<String, LoginRequest> loginRequests = new HashMap<>();

    // Plugin
    public PopupInterface plugin;

    // Request Cleaner
    BukkitTask requestCleaner;
}
