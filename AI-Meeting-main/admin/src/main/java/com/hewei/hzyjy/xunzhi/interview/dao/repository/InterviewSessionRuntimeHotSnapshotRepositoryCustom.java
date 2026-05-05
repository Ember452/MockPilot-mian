package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewSessionRuntimeHotPatch;

/**
 * 定义热快照字段级 Patch 持久化扩展接口，
 * 用于按 session 维度增量更新高频运行态字段。
 *
 * @author 程序员牛肉
 */
public interface InterviewSessionRuntimeHotSnapshotRepositoryCustom {

    void applyPatch(String sessionId, InterviewSessionRuntimeHotPatch patch);

    boolean compareAndSetPatch(String sessionId, Long expectedSnapshotVersion, InterviewSessionRuntimeHotPatch patch);
}
