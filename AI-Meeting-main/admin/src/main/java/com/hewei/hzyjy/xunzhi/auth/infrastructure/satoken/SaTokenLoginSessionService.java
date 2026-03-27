package com.hewei.hzyjy.xunzhi.auth.infrastructure.satoken;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.auth.application.LoginSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SaTokenLoginSessionService implements LoginSessionService {

    @Override
    public void login(String username) {
        StpUtil.login(username);
    }

    @Override
    public void logoutCurrent() {
        StpUtil.logout();
    }

    @Override
    public boolean isCurrentLoggedIn() {
        try {
            return StpUtil.isLogin();
        } catch (Exception ex) {
            log.error("Failed to check current login status: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String getCurrentToken() {
        try {
            return StpUtil.getTokenValue();
        } catch (Exception ex) {
            log.error("Failed to get current token: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public String getCurrentLoginId() {
        try {
            Object loginId = StpUtil.getLoginId();
            return loginId != null ? loginId.toString() : null;
        } catch (Exception ex) {
            log.error("Failed to get current login id: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public void logoutByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        try {
            StpUtil.logoutByTokenValue(token);
        } catch (Exception ex) {
            log.error("Failed to logout by token: {}", ex.getMessage());
        }
    }

    @Override
    public long getTokenTimeout(String token) {
        if (StrUtil.isBlank(token)) {
            return -2L;
        }
        try {
            return StpUtil.getTokenTimeout(token);
        } catch (Exception ex) {
            log.error("Failed to get token timeout: {}", ex.getMessage());
            return -2L;
        }
    }
}
