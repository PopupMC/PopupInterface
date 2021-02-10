package com.popupmc.popupinterface.auth;

// Contains an authentication response
public class AuthResponse {
    public AuthResponse(AuthResponseCodes code) {
        this.code = code;
    }

    public AuthResponse(AuthResponseCodes code, Object data) {
        this.code = code;
        this.data = data;
    }

    public AuthResponseCodes code;
    public Object data;
}
