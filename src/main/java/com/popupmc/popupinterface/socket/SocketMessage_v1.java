package com.popupmc.popupinterface.socket;

// Version 1 of a Socket Message
public class SocketMessage_v1 extends SocketMessage_Base {
    public SocketMessage_v1() {
        version = 1;
    }

    // Namespace (Group/Category) of actions wished to take
    public String ns;
    public String action;

    // Value or Values
    public Object value;
    public Object[] values;

    // Auth Token (Optional, but may be required)
    public String token;
}
