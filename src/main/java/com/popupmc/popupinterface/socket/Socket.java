package com.popupmc.popupinterface.socket;

import com.popupmc.popupinterface.PopupInterface;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Socket extends WebSocketServer {

    public Socket(InetSocketAddress address, PopupInterface plugin) {
        super(address);
        this.plugin = plugin;
        this.messageDecoder = new SocketMessageDecoder(plugin);
        this.messageProcessor = new SocketMessageProcessor(plugin);
    }

    @Override
    public void onStart() {
        //
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SocketMessage_Base msgBase = messageDecoder.decode(conn, message);
        if(msgBase == null)
            return;

        messageProcessor.processMsg(conn, msgBase);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        //
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        //
    }

    public PopupInterface plugin;
    public SocketMessageDecoder messageDecoder;
    public SocketMessageProcessor messageProcessor;
}
