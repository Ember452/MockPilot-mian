package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 提供面试会话运行态懒恢复过程中的分布式互斥锁能力，
 * 用于避免同一会话在并发恢复时重复重建缓存或产生状态覆盖。
 *
 * @author 程序员牛肉
 */
@Service
@RequiredArgsConstructor
public class InterviewSessionRuntimeLockService {

    private static final String LOCK_KEY_PREFIX = "interview:runtime:rehydrate:lock:";
    private static final long LOCK_WAIT_MILLIS = 0L;
    private static final long LOCK_LEASE_SECONDS = 60L;

    private final RedissonClient redissonClient;

    public RLock acquire(String sessionId) throws InterruptedException {
        return acquire(sessionId, LOCK_WAIT_MILLIS);
    }

    public RLock acquire(String sessionId, long waitMillis) throws InterruptedException {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + sessionId);
        boolean acquired = lock.tryLock(Math.max(waitMillis, 0L), LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        return acquired ? lock : null;
    }

    public void release(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
