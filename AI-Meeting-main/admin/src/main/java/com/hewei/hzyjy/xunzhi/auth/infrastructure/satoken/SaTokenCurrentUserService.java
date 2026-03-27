package com.hewei.hzyjy.xunzhi.auth.infrastructure.satoken;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hewei.hzyjy.xunzhi.auth.application.CurrentUserService;
import com.hewei.hzyjy.xunzhi.auth.domain.CurrentPrincipal;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.user.dao.entity.UserDO;
import com.hewei.hzyjy.xunzhi.user.dao.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Sa-Token backed current-user resolution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaTokenCurrentUserService implements CurrentUserService {

    private static final String USER_ID_CACHE_KEY = "xunzhi:user:id:";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    @Override
    public boolean isLoggedIn() {
        try {
            return StpUtil.isLogin();
        } catch (Exception ex) {
            log.error("Failed to check login status: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String getCurrentUsername() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object loginId = StpUtil.getLoginId();
            return loginId != null ? loginId.toString() : null;
        } catch (Exception ex) {
            log.error("Failed to get current username: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public Long getCurrentUserId() {
        String username = getCurrentUsername();
        return StrUtil.isBlank(username) ? null : getUserIdByUsername(username);
    }

    @Override
    public CurrentPrincipal getCurrentPrincipal() {
        String username = getCurrentUsername();
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return new CurrentPrincipal(getUserIdByUsername(username), username);
    }

    @Override
    public CurrentPrincipal requireCurrentPrincipal() {
        CurrentPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            throw new ClientException("User is not logged in");
        }
        return principal;
    }

    @Override
    public Long getUserIdByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new ClientException("username cannot be empty");
        }

        String cacheKey = USER_ID_CACHE_KEY + username;
        String cachedUserId = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedUserId)) {
            stringRedisTemplate.expire(cacheKey, 30L, TimeUnit.MINUTES);
            return Long.valueOf(cachedUserId);
        }

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .select(UserDO::getId);

        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null || userDO.getId() == null) {
            throw new ClientException("user does not exist: " + username);
        }

        stringRedisTemplate.opsForValue().set(cacheKey, userDO.getId().toString(), 30L, TimeUnit.MINUTES);
        return userDO.getId();
    }

    @Override
    public String getUsernameByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null ? loginId.toString() : null;
        } catch (Exception ex) {
            log.warn("Failed to parse username from token: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader(StpUtil.getTokenName());
        if (StrUtil.isNotBlank(token)) {
            return trimBearerPrefix(token);
        }
        token = request.getParameter(StpUtil.getTokenName());
        if (StrUtil.isNotBlank(token)) {
            return trimBearerPrefix(token);
        }
        return null;
    }

    @Override
    public boolean isValidToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            return StpUtil.getLoginIdByToken(token) != null;
        } catch (Exception ex) {
            log.warn("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    private String trimBearerPrefix(String token) {
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
