package com.popupmc.popupinterface.auth;

import com.popupmc.popupinterface.PopupInterface;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public class AuthSessionManager {
    public AuthSessionManager(PopupInterface plugin) {
        this.plugin = plugin;

        // Start session cleaner 30 minutes from now and have it run every 30 minutes
        // This is the cheaper alternative to using a Redis DB which is WAYYYYY better
        sessionCleaner = new BukkitRunnable() {
            @Override
            public void run() {
                cleanSessions();
            }
        }.runTaskTimerAsynchronously(plugin, 30 * 60 * 20, 30 * 60 * 20);
    }

    // Creates a session
    public AuthResponse createSession(UUID minecraftUUID, String discordUUID) {

        // Allow no more than 250 sessions in total
        // Thats 125 users using 2 sessions each or 250 users using 1 session each
        // Dont auto remove and try again as that allows a hacker to crash the server by spamming session creation
        if(sessions.size() > 250)
            return new AuthResponse(AuthResponseCodes.SERVER_SESSION_LIMIT);

        // Only allow 2 sessions per account
        int count = sessionCount.getOrDefault(discordUUID, 0);
        if(count >= 2)
            return new AuthResponse(AuthResponseCodes.USER_SESSON_LIMIT);

        // Create session
        AuthSession session = new AuthSession(minecraftUUID, discordUUID, plugin);

        // Save session & increment session count for that user
        sessions.put(session.sessionID, session);
        sessionCount.put(discordUUID, count + 1);

        return new AuthResponse(AuthResponseCodes.SESSION_CREATED, session.sessionID);
    }

    // Removes a session
    public AuthResponse removeSession(long id) {

        // Do nothing if theres no session with that id
        if(!this.sessions.containsKey(id))
            return new AuthResponse(AuthResponseCodes.SESSION_DOESNT_EXIST, id);

        // Get the session
        AuthSession sid = sessions.get(id);

        // Get session count of the player
        int count = sessionCount.getOrDefault(sid.discordUUID, 0);

        // Remove if 0 or below, otherwise decrement
        if(count <= 0)
            sessionCount.remove(sid.discordUUID);
        else
            sessionCount.put(sid.discordUUID, count - 1);

        // Remove session
        sessions.remove(id);

        return new AuthResponse(AuthResponseCodes.SESSION_REMOVED, id);
    }

    // Clean expired sessions
    public void cleanSessions() {
        for(AuthSession session : sessions.values()) {
            if(session.hasExpired())
                removeSession(session.sessionID);
        }
    }

    // Plugin
    public PopupInterface plugin;

    // All the sessions indexed by session ID
    public HashMap<Long, AuthSession> sessions = new HashMap<>();
    public HashMap<String, Integer> sessionCount = new HashMap<>();

    // Session Cleaner
    BukkitTask sessionCleaner;
}
