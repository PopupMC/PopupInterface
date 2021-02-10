package com.popupmc.popupinterface.socket;

import com.popupmc.popupinterface.discord.LoginRequest;

public interface LoginRejectCB {
    void reject(LoginRequest request);
}
