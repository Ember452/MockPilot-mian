package com.hewei.hzyjy.xunzhi.auth.application;

public interface LoginSessionService {

    void login(String username);

    void logoutCurrent();

    boolean isCurrentLoggedIn();

    String getCurrentToken();

    String getCurrentLoginId();

    void logoutByToken(String token);

    long getTokenTimeout(String token);
}
