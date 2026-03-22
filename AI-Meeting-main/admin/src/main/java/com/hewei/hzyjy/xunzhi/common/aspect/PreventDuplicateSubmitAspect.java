package com.hewei.hzyjy.xunzhi.common.aspect;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiMessageReqDTO;
import com.hewei.hzyjy.xunzhi.auth.application.CurrentUserService;
import com.hewei.hzyjy.xunzhi.common.annotation.PreventDuplicateSubmit;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Duplicate-submit guard backed by a Redisson distributed lock.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PreventDuplicateSubmitAspect {

    private final RedissonClient redissonClient;
    private final CurrentUserService currentUserService;

    @Around("@annotation(preventDuplicateSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, PreventDuplicateSubmit preventDuplicateSubmit) throws Throwable {
        String lockKey = buildLockKey(joinPoint, preventDuplicateSubmit);
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    preventDuplicateSubmit.waitTime(),
                    preventDuplicateSubmit.expireTime(),
                    TimeUnit.SECONDS
            );
            if (!acquired) {
                log.warn("Duplicate submit intercepted, lockKey={}", lockKey);
                throw new ClientException(preventDuplicateSubmit.message());
            }

            log.debug("Acquired duplicate-submit lock, lockKey={}", lockKey);
            return joinPoint.proceed();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ClientException("系统繁忙，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released duplicate-submit lock, lockKey={}", lockKey);
            }
        }
    }

    private String buildLockKey(ProceedingJoinPoint joinPoint, PreventDuplicateSubmit annotation) {
        StringBuilder keyBuilder = new StringBuilder(annotation.prefix());
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();

        String username = null;
        String sessionId = null;
        Integer messageSeq = null;

        if (annotation.userLevel()) {
            username = currentUserService.getCurrentUsername();
        }

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if ("sessionId".equals(paramNames[i]) && arg instanceof String value) {
                sessionId = value;
            }
            if (arg instanceof AiMessageReqDTO reqDTO) {
                if (annotation.sessionLevel() && StrUtil.isNotBlank(reqDTO.getSessionId())) {
                    sessionId = reqDTO.getSessionId();
                }
                if (annotation.messageSeqLevel() && reqDTO.getMessageSeq() != null) {
                    messageSeq = reqDTO.getMessageSeq();
                }
            }
        }

        if (annotation.userLevel() && StrUtil.isNotBlank(username)) {
            keyBuilder.append(":").append(username);
        }
        if (annotation.sessionLevel() && StrUtil.isNotBlank(sessionId)) {
            keyBuilder.append(":").append(sessionId);
        }
        if (annotation.messageSeqLevel() && messageSeq != null) {
            keyBuilder.append(":").append(messageSeq);
        }
        if (keyBuilder.toString().equals(annotation.prefix())) {
            keyBuilder.append(":").append(method.getName());
        }
        return keyBuilder.toString();
    }
}
