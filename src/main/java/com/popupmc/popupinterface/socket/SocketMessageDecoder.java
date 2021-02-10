package com.popupmc.popupinterface.socket;

import com.google.gson.JsonSyntaxException;
import com.popupmc.popupinterface.PopupInterface;
import org.java_websocket.WebSocket;

public class SocketMessageDecoder {
    public SocketMessageDecoder(PopupInterface plugin) {
        this.plugin = plugin;
    }

    public SocketMessage_Base decode(WebSocket conn, String message) {
        int msgVer = getVersion(conn, message);

        if(msgVer == 1)
            return decodeV1(conn, message);

        return null;
    }

    public void cantDecodeReply(WebSocket conn) {

        SocketMessage_v1 msg = new SocketMessage_v1();
        msg.ns = "error/decode";
        msg.value = "Unable to decode your message";

        conn.send(plugin.json.toJson(msg));
    }

    public int getVersion(WebSocket conn, String message) {
        SocketMessage_Base msg;

        try {
            msg = plugin.json.fromJson(message, SocketMessage_Base.class);
        }
        catch (JsonSyntaxException ex) {
            cantDecodeReply(conn);
            return -1;
        }

        return msg.version;
    }

    public SocketMessage_v1 decodeV1(WebSocket conn, String message) {
        SocketMessage_v1 msg;

        try {
            msg = plugin.json.fromJson(message, SocketMessage_v1.class);
        }
        catch (JsonSyntaxException ex) {
            cantDecodeReply(conn);
            return null;
        }

        return msg;
    }

    PopupInterface plugin;
}
