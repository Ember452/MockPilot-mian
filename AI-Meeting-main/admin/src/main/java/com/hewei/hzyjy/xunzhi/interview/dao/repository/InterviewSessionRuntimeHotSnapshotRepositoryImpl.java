package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewSessionRuntimeHotPatch;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeHotSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.result.UpdateResult;

import java.util.Date;

/**
 * 提供面试会话热快照字段级 Patch 的 Mongo 实现，
 * 用于通过 sessionId 对高频运行态做最小字段更新而不是整包覆盖。
 *
 * @author 程序员牛肉
 */
@RequiredArgsConstructor
@Repository
public class InterviewSessionRuntimeHotSnapshotRepositoryImpl implements InterviewSessionRuntimeHotSnapshotRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void applyPatch(String sessionId, InterviewSessionRuntimeHotPatch patch) {
        if (StrUtil.isBlank(sessionId) || patch == null) {
            return;
        }
        Date now = new Date();
        Query query = Query.query(Criteria.where("sessionId").is(sessionId.trim()));
        Update update = buildUpdate(sessionId, patch, now, true);
        mongoTemplate.upsert(query, update, InterviewSessionRuntimeHotSnapshot.class);
    }

    @Override
    public boolean compareAndSetPatch(String sessionId, Long expectedSnapshotVersion, InterviewSessionRuntimeHotPatch patch) {
        if (StrUtil.isBlank(sessionId) || expectedSnapshotVersion == null || patch == null) {
            return false;
        }
        Date now = new Date();
        Query query = Query.query(Criteria.where("sessionId").is(sessionId.trim())
                .and("snapshotVersion").is(expectedSnapshotVersion));
        Update update = buildUpdate(sessionId, patch, now, false);
        UpdateResult result = mongoTemplate.updateFirst(query, update, InterviewSessionRuntimeHotSnapshot.class);
        return result != null && result.getModifiedCount() > 0;
    }

    private Update buildUpdate(String sessionId, InterviewSessionRuntimeHotPatch patch, Date now, boolean withOnInsert) {
        Update update = new Update();
        if (withOnInsert) {
            update.setOnInsert("sessionId", sessionId.trim());
            update.setOnInsert("createTime", now);
        }
        if (patch.getUserId() != null) {
            update.set("userId", patch.getUserId());
        }
        if (StrUtil.isNotBlank(patch.getSessionStatus())) {
            update.set("sessionStatus", patch.getSessionStatus().trim());
        }
        if (patch.getSnapshotVersion() != null) {
            update.set("snapshotVersion", patch.getSnapshotVersion());
        }
        if (StrUtil.isNotBlank(patch.getSnapshotLevel())) {
            update.set("snapshotLevel", patch.getSnapshotLevel().trim());
        }
        if (patch.getRebuildConfidence() != null) {
            update.set("rebuildConfidence", patch.getRebuildConfidence());
        }
        if (patch.getSnapshotUpdatedAt() != null) {
            update.set("snapshotUpdatedAt", patch.getSnapshotUpdatedAt());
        }
        if (patch.getFlow() != null) {
            update.set("flow", patch.getFlow());
        }
        if (patch.getScoreAggregate() != null) {
            update.set("scoreAggregate", patch.getScoreAggregate());
        }
        if (patch.getFollowUpQuestions() != null) {
            update.set("followUpQuestions", patch.getFollowUpQuestions());
        }
        if (patch.getRecentTurns() != null) {
            update.set("recentTurns", patch.getRecentTurns());
        }
        if (patch.getRecentTurnCount() != null) {
            update.set("recentTurnCount", patch.getRecentTurnCount());
        }
        if (patch.getArchiveWatermark() != null) {
            update.set("archiveWatermark", patch.getArchiveWatermark());
        }
        if (patch.getLastTurnSeq() != null) {
            update.set("lastTurnSeq", patch.getLastTurnSeq());
        }
        if (StrUtil.isNotBlank(patch.getLastAppliedRequestId())) {
            update.set("lastAppliedRequestId", patch.getLastAppliedRequestId().trim());
        }
        if (StrUtil.isNotBlank(patch.getLastMutationId())) {
            update.set("lastMutationId", patch.getLastMutationId().trim());
        }
        if (patch.getLastMutationTime() != null) {
            update.set("lastMutationTime", patch.getLastMutationTime());
        }
        if (StrUtil.isNotBlank(patch.getLastCommittedQuestionNumber())) {
            update.set("lastCommittedQuestionNumber", patch.getLastCommittedQuestionNumber().trim());
        }
        if (StrUtil.isNotBlank(patch.getLastCommittedTurnDigest())) {
            update.set("lastCommittedTurnDigest", patch.getLastCommittedTurnDigest().trim());
        }
        update.set("updateTime", now);
        return update;
    }
}
