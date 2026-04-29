package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.config.InterviewHotRefreshConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 提供面试会话热快照合并写与防抖刷新的本地协调能力，
 * 用于按 session 聚合热刷新意图、延迟普通中间态刷盘并保证关键检查点立即落盘。
 *
 * @author 程序员牛肉
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionRuntimeHotRefreshCoordinator {

    private final InterviewHotRefreshConfiguration hotRefreshConfiguration;

    private final InterviewSessionRuntimeSnapshotService runtimeSnapshotService;

    @Qualifier("scheduledExecutorService")
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, InterviewSessionRuntimeHotRefreshBucket> pendingBuckets = new ConcurrentHashMap<>();

    public void submit(InterviewSessionRuntimeHotRefreshRequest request) {
        if (request == null || StrUtil.isBlank(request.getSessionId())) {
            return;
        }
        if (!Boolean.TRUE.equals(hotRefreshConfiguration.getEnable())) {
            runtimeSnapshotService.flushHotRefreshRequest(request);
            return;
        }
        if (request.isForceFlush()) {
            submitForceFlush(request);
            return;
        }
        submitDeferred(request);
    }

    private void submitDeferred(InterviewSessionRuntimeHotRefreshRequest request) {
        InterviewSessionRuntimeHotRefreshBucket bucket = pendingBuckets.computeIfAbsent(
                request.getSessionId(),
                InterviewSessionRuntimeHotRefreshBucket::new
        );
        synchronized (bucket) {
            bucket.merge(request);
            if (bucket.isFlushInProgress()) {
                return;
            }
            scheduleFlushLocked(bucket, computeDebounceDelayLocked(bucket));
        }
    }

    private void submitForceFlush(InterviewSessionRuntimeHotRefreshRequest request) {
        InterviewSessionRuntimeHotRefreshBucket bucket = pendingBuckets.computeIfAbsent(
                request.getSessionId(),
                InterviewSessionRuntimeHotRefreshBucket::new
        );
        boolean flushNow = false;
        synchronized (bucket) {
            bucket.merge(request);
            bucket.clearScheduledFuture();
            long waitDeadline = System.currentTimeMillis() + positive(hotRefreshConfiguration.getForceFlushWaitMillis(), 600L);
            while (bucket.isFlushInProgress() && System.currentTimeMillis() < waitDeadline) {
                try {
                    long remaining = waitDeadline - System.currentTimeMillis();
                    bucket.wait(Math.min(Math.max(remaining, 1L), 50L));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            flushNow = !bucket.isFlushInProgress();
        }
        if (flushNow) {
            flushBucket(request.getSessionId(), bucket);
        }
    }

    private void flushBucket(String sessionId, InterviewSessionRuntimeHotRefreshBucket bucket) {
        int immediateAttempts = 0;
        while (true) {
            InterviewSessionRuntimeHotRefreshRequest flushRequest;
            synchronized (bucket) {
                flushRequest = bucket.startFlush();
                if (flushRequest == null) {
                    cleanupIfIdle(sessionId, bucket);
                    return;
                }
            }
            boolean success = runtimeSnapshotService.flushHotRefreshRequest(flushRequest);
            boolean continueImmediately = false;
            synchronized (bucket) {
                bucket.completeFlush();
                bucket.notifyAll();
                if (!success) {
                    bucket.restoreFailedRequest(flushRequest);
                }
                if (!bucket.hasPendingRefresh()) {
                    cleanupIfIdle(sessionId, bucket);
                    return;
                }
                if ((bucket.isPendingForceFlush() || !success)
                        && immediateAttempts < positive(hotRefreshConfiguration.getMaxImmediateFlushAttempts(), 2)) {
                    continueImmediately = true;
                    immediateAttempts++;
                } else {
                    long scheduleDelay = success
                            ? computeDebounceDelayLocked(bucket)
                            : positive(hotRefreshConfiguration.getFailureRetryDelayMillis(), 200L);
                    scheduleFlushLocked(bucket, scheduleDelay);
                }
            }
            if (!continueImmediately) {
                return;
            }
        }
    }

    private void scheduleFlushLocked(InterviewSessionRuntimeHotRefreshBucket bucket, long delayMillis) {
        bucket.clearScheduledFuture();
        long safeDelay = Math.max(delayMillis, 0L);
        long scheduledAt = System.currentTimeMillis() + safeDelay;
        ScheduledFuture<?> future = scheduledExecutorService.schedule(
                () -> flushBucket(bucket.getSessionId(), bucket),
                safeDelay,
                TimeUnit.MILLISECONDS
        );
        bucket.attachScheduledFuture(future, scheduledAt);
    }

    private long computeDebounceDelayLocked(InterviewSessionRuntimeHotRefreshBucket bucket) {
        long now = System.currentTimeMillis();
        long scheduledAt = bucket.computeScheduledFlushAt(
                now,
                positive(hotRefreshConfiguration.getDebounceWindowMillis(), 150L),
                positive(hotRefreshConfiguration.getMaxAggregateWindowMillis(), 500L)
        );
        return Math.max(scheduledAt - now, 0L);
    }

    private void cleanupIfIdle(String sessionId, InterviewSessionRuntimeHotRefreshBucket bucket) {
        synchronized (bucket) {
            if (bucket.isFlushInProgress() || bucket.hasPendingRefresh()) {
                return;
            }
            if (bucket.getScheduledFuture() != null && !bucket.getScheduledFuture().isDone()) {
                return;
            }
        }
        pendingBuckets.remove(sessionId, bucket);
    }

    private long positive(Long value, long defaultValue) {
        return value != null && value > 0L ? value : defaultValue;
    }

    private int positive(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }
}
