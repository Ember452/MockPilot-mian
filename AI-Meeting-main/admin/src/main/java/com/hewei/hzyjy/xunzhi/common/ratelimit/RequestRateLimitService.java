package com.hewei.hzyjy.xunzhi.common.ratelimit;

public interface RequestRateLimitService {

    /**
     * 尝试获取令牌
     */
    boolean tryAcquire(String key, RequestRateLimitPolicy policy);
}
