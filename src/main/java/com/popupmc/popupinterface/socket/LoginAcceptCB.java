package com.popupmc.popupinterface.socket;

import com.popupmc.popupinterface.discord.LoginRequest;

public interface LoginAcceptCB {
    void accept(LoginRequest request);
}
