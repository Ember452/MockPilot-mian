package com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.service;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.application.guard.core.InterviewAiGuardException;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.cache.FlightReplayLocalCache;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.cache.FlightResultSerializer;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.coordinator.FlightCoordinatorRepository;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.coordinator.FlightHeartbeatManager;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.coordinator.FlightNotificationService;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightAcquireResult;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightErrorType;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightMetaSnapshot;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightMode;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightOwnerContext;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightStatus;
import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightStoredResult;
import com.hewei.hzyjy.xunzhi.interview.config.InterviewAiSingleFlightConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 分布式 AI single-flight 核心服务，负责在集群内协调 owner 与 follower，
 * 完成请求抢占、结果复用、失败接管以及本地降级回退。
 *
 * @author solis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedInterviewAiSingleFlightService {

    private final InterviewAiSingleFlightConfiguration configuration;
    private final InterviewAiSingleFlightService localSingleFlightService;
    private final FlightCoordinatorRepository flightCoordinatorRepository;
    private final FlightNotificationService flightNotificationService;
    private final FlightHeartbeatManager flightHeartbeatManager;
    private final FlightResultSerializer flightResultSerializer;
    private final FlightReplayLocalCache flightReplayLocalCache;

    /**
     * 执行分布式 AI single-flight
     */
    public String execute(String stage, String requestKey, Supplier<String> supplier) {
        // 刷新缓存最大容量
        flightReplayLocalCache.refreshMaxSize(configuration.getL1CacheMaxSize());
        FlightMode mode = FlightMode.from(configuration.normalizedMode());
        // 如果配置中没有开启的话，调用本地SingleFlight
        if (!Boolean.TRUE.equals(configuration.getEnable()) || mode == FlightMode.LOCAL || !Boolean.TRUE.equals(configuration.getDistributedEnabled())) {
            return localSingleFlightService.execute(requestKey, supplier);
        }
        try {
            return executeDistributed(stage, requestKey, supplier);
        } catch (RuntimeException ex) {
            // 分布式调用失败，降级为本地SingleFlight
            if (mode == FlightMode.HYBRID) {
                log.warn("Distributed single-flight fallback to local mode, stage={}, key={}, reason={}", stage, requestKey, ex.getMessage());
                return localSingleFlightService.execute(requestKey, supplier);
            }
            throw ex;
        }
    }

    /**
     * 执行分布式 AI single-flight
     * @param stage 阶段
     * @param requestKey 请求key
     * @param supplier 待执行的函数
     * @return 执行结果
     */
    private String executeDistributed(String stage, String requestKey, Supplier<String> supplier) {
        String safeStage = StrUtil.blankToDefault(stage, "interview-default");
        String safeRequestKey = StrUtil.blankToDefault(requestKey, safeStage + "|no-key");
        InterviewAiSingleFlightConfiguration.StageFlightPolicy policy = configuration.resolveStagePolicy(safeStage);
        // 如果本地缓存中有结果：复用
        String localReplay = flightReplayLocalCache.get(safeStage, safeRequestKey);
        if (localReplay != null) {
            return localReplay;
        }
        // 设置follower的等待超时时间
        long deadline = System.currentTimeMillis() + resolveFollowerMaxWaitMillis();
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            FlightAcquireResult acquireResult = flightCoordinatorRepository.acquireOrJoin(
                    safeStage,
                    safeRequestKey,
                    nodeId(),
                    extractSessionId(safeRequestKey),
                    policy
            );
            if (acquireResult == null || acquireResult.getAction() == null) {
                return localSingleFlightService.execute(safeRequestKey, supplier);
            }
            switch (acquireResult.getAction()) {
                case OWNER_NEW, OWNER_TAKEOVER -> {
                    return ownerExecute(safeStage, safeRequestKey, acquireResult.getOwnerToken(), supplier, policy);
                }
                case REPLAY_SUCCESS -> {
                    String replay = tryReadSuccessReplay(safeStage, safeRequestKey, policy);
                    if (replay != null) {
                        return replay;
                    }
                }
                case REPLAY_FAILURE -> throw replayFailure(acquireResult);
                case FOLLOWER_WAIT -> {
                    String followerReplay = followerWait(safeStage, safeRequestKey, policy, deadline);
                    if (followerReplay != null) {
                        return followerReplay;
                    }
                }
                default -> {
                    return localSingleFlightService.execute(safeRequestKey, supplier);
                }
            }
        }
        throw new CompletionException(new RejectedExecutionException("distributed single-flight max attempts exceeded"));
    }

    /**
     * 以 owner（执行者）身份执行分布式 single-flight 请求。<p>
     *
     * 核心逻辑：<br>
     * 1. 在协调器（Redis）中标记本节点为 running 状态，若标记失败则降级为 follower 等待；<br>
     * 2. 启动定时心跳，维持 owner 身份避免被其它节点接管；<br>
     * 3. 执行实际业务逻辑 {@code supplier.get()}；<br>
     * 4. 序列化结果并写入协调器，同时将元数据状态标记为 SUCCEEDED；<br>
     * 5. 通过通知服务发布成功事件，唤醒所有等待的 follower；<br>
     * 6. 将结果缓存至本地 L1 缓存，加速同节点后续请求复用；<br>
     * 7. 若业务执行或状态写入选入异常，则归类后写入失败状态并通知 follower，最后重新抛出异常；<br>
     * 8. finally 中无论成功或失败，均停止心跳。<br>
     *
     * @param stage      阶段标识（如 interview-qa、interview-summary 等）
     * @param requestKey 请求键，标识同一个需要折叠的请求
     * @param ownerToken 当前 owner 持有的不重复令牌，用于防误写
     * @param supplier   真正的业务逻辑提供者
     * @param policy     该阶段对应的 single-flight 策略配置
     * @return 业务执行结果
     */
    private String ownerExecute(String stage, String requestKey, Long ownerToken,
                                Supplier<String> supplier,
                                InterviewAiSingleFlightConfiguration.StageFlightPolicy policy) {
        // 从策略中获取 running 状态的 TTL，默认 15 秒；若超过此时间 owner 未心跳续期，其它节点可接管
        long runningTtlMillis = positive(policy.getRunningTtlMillis(), 15000L);

        // 在协调器（Redis）中标记本节点为该请求的 running owner
        // 使用 NX + 原子写入，只有首次成功才算数；若已存在则返回 false
        boolean markedRunning = flightCoordinatorRepository.markRunning(requestKey, nodeId(), ownerToken, runningTtlMillis);

        // 标记失败 → 说明已有其它节点抢先成为 owner，本节点降级为 follower 等待其执行完成
        if (!markedRunning) {
            return followerWait(stage, requestKey, policy, System.currentTimeMillis() + resolveFollowerMaxWaitMillis());
        }

        // 构造 owner 上下文，供心跳管理器使用
        FlightOwnerContext ownerContext = FlightOwnerContext.builder()
                .stage(stage)
                .requestKey(requestKey)
                .ownerId(nodeId())
                .ownerToken(ownerToken)
                .policy(policy)
                .build();

        // 启动定时心跳任务，在 runningTtlMillis 周期内持续续期，向协调器证明本节点仍存活
        String heartbeatTaskKey = flightHeartbeatManager.start(
                ownerContext,
                () -> flightCoordinatorRepository.heartbeat(requestKey, nodeId(), ownerToken, runningTtlMillis)
        );

        try {
            // ==================== 成功路径 ====================
            // 执行真正的 AI 调用（出题、评分、追问等）
            String result = supplier.get();

            // 将业务执行结果序列化为可持久化的格式，同时携带 ownerToken 防误写
            FlightStoredResult storedResult = flightResultSerializer.serialize(result, ownerToken, policy);
            long resultTtlMillis = positive(policy.getResultTtlMillis(), 600000L); // 结果有效期默认 10 分钟

            // 将序列化后的结果写入协调器（Redis），供后续的 follower 或其它节点复用
            if (!flightCoordinatorRepository.storeResult(requestKey, nodeId(), ownerToken, storedResult, resultTtlMillis)) {
                // 写入失败 → 抛出异常，进入 catch 块清理 owner 状态
                throw new IllegalStateException("failed to store distributed flight result");
            }

            // 在协调器中将该请求的状态标记为 SUCCEEDED，同时保留结果数据 resultTtlMillis 时长
            if (!flightCoordinatorRepository.finishSuccess(requestKey, nodeId(), ownerToken, resultTtlMillis)) {
                // 标记失败（可能协调器已被其它节点覆盖），尝试从协调器中读取已有成功结果
                String replay = tryReadSuccessReplay(stage, requestKey, policy);
                if (replay != null) {
                    return replay; // 成功读到其它 owner 写的结果，直接返回
                }
                // 既无法标记成功又读不到任何成功结果 → 抛出异常
                throw new IllegalStateException("failed to finish distributed flight success state");
            }

            // 通知所有正在等待该请求的 follower：owner 已执行成功，它们可以读取结果继续处理了
            flightNotificationService.publish(requestKey, "owner_succeeded", FlightStatus.SUCCEEDED, ownerToken, null, false);

            // 将结果放入本地 L1 缓存，下一次相同请求可在本节点直接命中，不必再走分布式协调
            flightReplayLocalCache.put(stage, requestKey, result, policy);

            return result;

        } catch (Throwable ex) {
            // ==================== 失败路径 ====================
            // 对捕获的异常进行分类，识别超时、过载、校验错误、未知异常等
            FlightFailure failure = classifyFailure(ex);

            // 在协调器中写入失败状态，包含错误类型、错误码、是否可重试等信息
            flightCoordinatorRepository.finishFailure(
                    requestKey,
                    nodeId(),
                    ownerToken,
                    failure.errorType,
                    failure.errorCode,
                    failure.retryable,
                    positive(policy.getFailedResultTtlMillis(), 60000L) // 失败结果默认保留 60 秒
            );

            // 通知所有 follower：owner 执行失败，它们根据失败信息决定重试或直接抛出
            flightNotificationService.publish(requestKey, "owner_failed", FlightStatus.FAILED, ownerToken, failure.errorType, failure.retryable);

            // 重新抛出异常，由上层决定是否降级为本地 single-flight（HYBRID 模式）
            throw rethrow(ex);

        } finally {
            // ==================== 最终清理 ====================
            // 无论成功或失败，都停止心跳，释放定时器资源
            flightHeartbeatManager.stop(heartbeatTaskKey);
        }
    }

    /**
     * 等待分布式 single-flight 请求执行完成，并返回结果。
     */
    private String followerWait(String stage, String requestKey,
                                InterviewAiSingleFlightConfiguration.StageFlightPolicy policy,
                                long deadlineMillis) {
        long streamBlockTimeoutMillis = positive(configuration.getStreamBlockTimeoutMillis(), 3000L);
        long pollIntervalMillis = positive(configuration.getPollFallbackIntervalMillis(), 2000L);
        long nextPollAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadlineMillis) {
            //尝试获取成功结果
            String replay = tryReadSuccessReplay(stage, requestKey, policy);
            if (replay != null) {
                return replay;
            }
            // 获取元数据快照，检查是否失败且不可重试
            FlightMetaSnapshot metaSnapshot = flightCoordinatorRepository.getMeta(requestKey);
            if (metaSnapshot != null && metaSnapshot.getStatus() == FlightStatus.FAILED && !Boolean.TRUE.equals(metaSnapshot.getRetryable())) {
                throw new IllegalStateException("distributed single-flight previous failure: "
                        + (metaSnapshot.getErrorCode() == null ? "FAILED" : metaSnapshot.getErrorCode()));
            }
            long remainingMillis = deadlineMillis - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                return null;
            }

            flightNotificationService.waitForTerminalEvent(requestKey, Math.min(streamBlockTimeoutMillis, remainingMillis));
            if (System.currentTimeMillis() >= nextPollAt) {
                // 尝试轮询获取成功结果
                String polledReplay = tryReadSuccessReplay(stage, requestKey, policy);
                if (polledReplay != null) {
                    return polledReplay;
                }
                // 更新下次轮询时间，当前时间加上轮询间隔
                nextPollAt = System.currentTimeMillis() + pollIntervalMillis;
            }
        }
        return null;
    }

    /**
     * 尝试读取成功回放结果
     * 1. 从本地L1缓存中查询读取
     * 2. 如果本地缓存中不存在，尝试从协调器中获取metadata
     * 3. 如果metadata中状态为成功，则从协调器中获取结果数据
     * 4. 如果结果数据存在，则从结果数据中反序列化结果并返回并存到本地缓存中
     * 5. 如果结果数据不存在，则返回null
     */
    private String tryReadSuccessReplay(String stage, String requestKey,
                                        InterviewAiSingleFlightConfiguration.StageFlightPolicy policy) {
        String localReplay = flightReplayLocalCache.get(stage, requestKey);
        if (localReplay != null) {
            return localReplay;
        }
        FlightMetaSnapshot metaSnapshot = flightCoordinatorRepository.getMeta(requestKey);
        if (metaSnapshot == null || metaSnapshot.getStatus() != FlightStatus.SUCCEEDED) {
            return null;
        }
        FlightStoredResult storedResult = flightCoordinatorRepository.getStoredResult(requestKey);
        if (storedResult == null) {
            return null;
        }
        String replay = flightResultSerializer.deserialize(storedResult);
        flightReplayLocalCache.put(stage, requestKey, replay, policy);
        return replay;
    }

    private RuntimeException replayFailure(FlightAcquireResult acquireResult) {
        boolean retryable = Boolean.TRUE.equals(acquireResult.getRetryable());
        String message = "distributed single-flight replay failure";
        if (StrUtil.isNotBlank(acquireResult.getErrorCode())) {
            message = message + ": " + acquireResult.getErrorCode();
        }
        if (retryable) {
            return new CompletionException(new RejectedExecutionException(message));
        }
        return new IllegalStateException(message);
    }

    private RuntimeException rethrow(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new CompletionException(throwable);
    }

    private FlightFailure classifyFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof InterviewAiGuardException guardException) {
            return switch (guardException.getErrorCode()) {
                case AI_TIMEOUT -> new FlightFailure(FlightErrorType.TIMEOUT, guardException.getErrorCode().name(), true);
                case AI_OVERLOADED -> new FlightFailure(FlightErrorType.OVERLOAD, guardException.getErrorCode().name(), true);
                case AI_UNAVAILABLE -> new FlightFailure(FlightErrorType.PROVIDER, guardException.getErrorCode().name(), true);
            };
        }
        if (cause instanceof TimeoutException) {
            return new FlightFailure(FlightErrorType.TIMEOUT, "TIMEOUT", true);
        }
        if (cause instanceof RejectedExecutionException) {
            return new FlightFailure(FlightErrorType.OVERLOAD, "OVERLOADED", true);
        }
        if (cause instanceof IllegalArgumentException) {
            return new FlightFailure(FlightErrorType.VALIDATION, "VALIDATION", false);
        }
        return new FlightFailure(FlightErrorType.UNEXPECTED, "UNEXPECTED", false);
    }

    private long resolveFollowerMaxWaitMillis() {
        return positive(configuration.getFollowerMaxWaitMillis(), 20000L);
    }

    /**
     * 返回非负值，优先使用传入的值，如果传入的值为负数则使用默认值。
     */
    private long positive(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private String extractSessionId(String requestKey) {
        if (StrUtil.isBlank(requestKey)) {
            return null;
        }
        String[] parts = requestKey.split("\\|");
        return parts.length > 1 ? parts[1] : null;
    }

    private String nodeId() {
        return Holder.NODE_ID;
    }

    /**
     * 懒加载当前节点标识的内部工具类，用于生成 owner 节点身份。
     *
     * @author 程序员牛肉
     */
    private static final class Holder {
        private static final String NODE_ID = resolveNodeId();

        private static String resolveNodeId() {
            try {
                return InetAddress.getLocalHost().getHostName() + "@" + ManagementFactory.getRuntimeMXBean().getName();
            } catch (UnknownHostException ex) {
                return ManagementFactory.getRuntimeMXBean().getName();
            }
        }
    }

    /**
     * 分布式协调过程中对异常进行归类后的内部失败对象，
     * 用于统一写入失败状态并决定是否允许重试接管。
     *
     * @author 程序员牛肉
     */
    private record FlightFailure(FlightErrorType errorType, String errorCode, boolean retryable) {
    }
}