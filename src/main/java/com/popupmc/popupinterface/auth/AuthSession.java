package com.popupmc.popupinterface.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.popupmc.popupinterface.PopupInterface;

import java.security.SecureRandom;
import java.time.*;
import java.util.Date;
import java.util.UUID;

public class AuthSession {

    public AuthSession(UUID minecraftUUID, String discordUUID, PopupInterface plugin) {
        this.plugin = plugin;
        this.discordUUID = discordUUID;
        this.minecraftUUID = minecraftUUID;

        // Set expiration time
        renewExpiration();

        // Generate JWT signing key for this session
        generateSessionSecret();

        // Generate session verifier to verify keys
        generateSessionVerifier();

        // Assign a session ID
        sessionID = sessionCounter;
        sessionCounter++;
    }

    // Renews session for 30 minutes
    public void renewExpiration() {
        // Get UTC Time & add 30 minutes
        LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.UTC);
        dateTime = dateTime.plusMinutes(30);

        // Set expiration
        expiration = dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    // Obtains expiration in the form of UTC date
    public LocalDateTime getExpiration() {
        return Instant.ofEpochMilli(expiration).atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    public Date getExpirationDate() {
        return Date.from(Instant.ofEpochMilli(expiration));
    }

    // Returns whether expired or not
    public boolean hasExpired() {
        long dateTimeNow = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
        return dateTimeNow >= expiration;
    }

    // Generates a 192 hexadecimal secret key for this session
    public void generateSessionSecret() {

        StringBuilder secret = new StringBuilder();

        SecureRandom rnd = new SecureRandom();

        for(int i = 0; i < 192; i++) {
            secret.append(secretChars[rnd.nextInt(secretChars.length)]);
        }

        // Save secret
        this.secret = secret.toString();

        // Convert to JWT algorithm
        secretAlgorithm = Algorithm.HMAC512(this.secret);
    }

    // JWT verification needs to be very strict
    public void generateSessionVerifier() {
        verifier = JWT.require(secretAlgorithm)

                // These are part of the government standard and thus highly reccomended
                .withIssuer("PopupMC")
                .withSubject("Authentication Session")
                .withAudience(discordUUID)
                .withJWTId(sessionID + "")

                // This is additional data to include
                .withClaim("minecraft", minecraftUUID.toString())
                .withClaim("discord", discordUUID)
                //.withClaim("expiration", expiration) // We don't use this because the expiration could have been extended
                .withClaim("session", sessionID)

                // Create builder
                .build();
    }

    // Creates a JWT token representing this session including all session data
    public String createSessionToken() {
        return JWT.create()
                // These are part of the government standard and thus highly reccomended
                .withIssuer("PopupMC")
                .withExpiresAt(getExpirationDate())
                .withSubject("Authentication Session")
                .withAudience(discordUUID)
                .withIssuedAt(Date.from(LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)))
                .withJWTId(sessionID + "")

                // This is additional data to include
                .withClaim("minecraft", minecraftUUID.toString())
                .withClaim("discord", discordUUID)
                .withClaim("expiration", expiration)
                .withClaim("session", sessionID)

                // Sign and Seal
                // The Token cannot be modified at this point, it's now read-only
                .sign(secretAlgorithm);
    }

    // Make sure the token is valid
    public boolean verifySessionToken(String token) {
        DecodedJWT jwt;

        try {
            jwt = verifier.verify(token);
        }
        catch (JWTVerificationException exception){
            return false;
        }

        // Check to see if the session has expired, this allows using old tokens to reference the same renewed
        // session
        return !hasExpired();
    }

    // Minecraft UUID Username
    public UUID minecraftUUID;

    // Discord UUID
    public String discordUUID;

    // Time Epoch when this session expires
    public long expiration;

    // Numeric Session ID
    public long sessionID;

    // Secret used to decode the JWT token
    public String secret;

    // JWT Algorithm
    public Algorithm secretAlgorithm;

    // JWT Verifier
    JWTVerifier verifier;

    // Session Counter to apply to new sessions
    public static long sessionCounter = 0;

    // Theres probably an easier way, idk it
    public final static char[] secretChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    // Server Plugin
    public PopupInterface plugin;
}
