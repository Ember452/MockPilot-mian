package com.hewei.hzyjy.xunzhi.auth.application;

import jakarta.websocket.Session;

public interface WebSocketAuthService {

    boolean isAuthorized(Session session, String pathUserId);
}
