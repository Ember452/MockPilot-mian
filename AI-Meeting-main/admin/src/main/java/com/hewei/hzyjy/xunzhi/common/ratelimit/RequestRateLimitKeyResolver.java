package com.hewei.hzyjy.xunzhi.common.ratelimit;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.auth.application.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves a stable rate-limit key for each request.
 */
@Component
@RequiredArgsConstructor
public class RequestRateLimitKeyResolver {

    private static final String USER_PREFIX = "user:";
    private static final String IP_PREFIX = "ip:";

    private final CurrentUserService currentUserService;

    public String resolve(HttpServletRequest request) {
        String token = currentUserService.extractToken(request);
        if (StrUtil.isNotBlank(token)) {
            String username = currentUserService.getUsernameByToken(token);
            if (StrUtil.isNotBlank(username)) {
                return USER_PREFIX + username;
            }
        }
        return IP_PREFIX + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            int separator = forwardedFor.indexOf(',');
            return separator >= 0 ? forwardedFor.substring(0, separator).trim() : forwardedFor;
        }

        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String remoteAddr = trimToNull(request.getRemoteAddr());
        return remoteAddr != null ? remoteAddr : "anonymous";
    }

    private String trimToNull(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        return "unknown".equalsIgnoreCase(normalized) ? null : normalized;
    }
}
