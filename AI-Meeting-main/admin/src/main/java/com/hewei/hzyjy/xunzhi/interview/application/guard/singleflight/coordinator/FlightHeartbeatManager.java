package com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.coordinator;

import com.hewei.hzyjy.xunzhi.interview.application.guard.singleflight.model.FlightOwnerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * owner 节点的 heartbeat 调度器，负责定时执行续租动作，
 * 保证长耗时 AI 请求在运行期间不会因为 TTL 到期而被误接管。
 *
 * @author 程序员牛肉
 */
@Service
@RequiredArgsConstructor
public class FlightHeartbeatManager {

    @Qualifier("scheduledExecutorService")
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    /**
     * 启动心跳任务
     * @param ownerContext owner节点执行AI请求的上下文对象，汇总当前阶段、请求键、owner 身份以及对应的 stage 策略。
     * @param heartbeatAction BooleanSupplier：好处：延迟执行，值在每次调用时动态决定
     * @return
     */
    public String start(FlightOwnerContext ownerContext, BooleanSupplier heartbeatAction) {
        long intervalMillis = ownerContext.getPolicy() == null || ownerContext.getPolicy().getHeartbeatIntervalMillis() == null
                ? 3000L
                : Math.max(500L, ownerContext.getPolicy().getHeartbeatIntervalMillis());
        String taskKey = ownerContext.getRequestKey() + "|" + ownerContext.getOwnerToken();
        // ScheduledFuture<?> future : 延时任务句柄，用来控制和管理已提交的定时任务
        // scheduledExecutorService.scheduleAtFixedRate：创建一个定长线程池，支持定时及周期性任务执行
        ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(
                () -> heartbeatAction.getAsBoolean(), // 任务内容（要执行的任务）
                intervalMillis,    // 初始延迟
                intervalMillis,    // 延迟时间
                TimeUnit.MILLISECONDS  // 时间单位
        );
        //用一个currentHashMap保存当前任务，方便后续停止
        //停止时直接将这个从Map中移除
        futures.put(taskKey, future);
        return taskKey;
    }

    public void stop(String taskKey) {
        if (taskKey == null) {
            return;
        }
        ScheduledFuture<?> future = futures.remove(taskKey);
        if (future != null) {
            future.cancel(true);
        }
    }
}
