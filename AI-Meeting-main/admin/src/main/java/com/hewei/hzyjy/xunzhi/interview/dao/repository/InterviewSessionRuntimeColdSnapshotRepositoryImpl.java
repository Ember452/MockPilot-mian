package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewSessionRuntimeColdPatch;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeColdSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * 提供面试会话冷快照字段级 Patch 的 Mongo 实现，
 * 用于通过 sessionId 对低频材料层做最小字段更新而不是整包覆盖。
 *
 * @author 程序员牛肉
 */
@RequiredArgsConstructor
@Repository
public class InterviewSessionRuntimeColdSnapshotRepositoryImpl implements InterviewSessionRuntimeColdSnapshotRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void applyPatch(String sessionId, InterviewSessionRuntimeColdPatch patch) {
        if (StrUtil.isBlank(sessionId) || patch == null) {
            return;
        }
        Date now = new Date();
        Query query = Query.query(Criteria.where("sessionId").is(sessionId.trim()));
        Update update = new Update();
        update.setOnInsert("sessionId", sessionId.trim());
        update.setOnInsert("createTime", now);
        if (patch.getUserId() != null) {
            update.set("userId", patch.getUserId());
        }
        if (patch.getMaterialVersion() != null) {
            update.set("materialVersion", patch.getMaterialVersion());
        }
        if (patch.getResumeFileUrl() != null) {
            update.set("resumeFileUrl", patch.getResumeFileUrl());
        }
        if (patch.getInterviewType() != null) {
            update.set("interviewType", patch.getInterviewType());
        }
        if (patch.getDirection() != null) {
            update.set("direction", patch.getDirection());
        }
        if (patch.getQuestions() != null) {
            update.set("questions", patch.getQuestions());
        }
        if (patch.getSuggestions() != null) {
            update.set("suggestions", patch.getSuggestions());
        }
        if (patch.getResumeContext() != null) {
            update.set("resumeContext", patch.getResumeContext());
        }
        if (patch.getResumeScore() != null) {
            update.set("resumeScore", patch.getResumeScore());
        }
        if (patch.getDemeanorScore() != null) {
            update.set("demeanorScore", patch.getDemeanorScore());
        }
        if (patch.getDemeanorDetails() != null) {
            update.set("demeanorDetails", patch.getDemeanorDetails());
        }
        if (patch.getMaterialUpdatedAt() != null) {
            update.set("materialUpdatedAt", patch.getMaterialUpdatedAt());
        }
        update.set("updateTime", now);
        mongoTemplate.upsert(query, update, InterviewSessionRuntimeColdSnapshot.class);
    }
}
