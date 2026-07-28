package com.hewei.hzyjy.xunzhi.interview.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ServiceException;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.service.UserModelPreferenceService;
import com.hewei.hzyjy.xunzhi.ai.service.chat.UniversalAiChatHandler;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.ReviewItemPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewPlaybackItemRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewRecordRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGenerateRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGrowthRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewItemRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewKbRefDTO;
import com.hewei.hzyjy.xunzhi.interview.config.ReviewProperties;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewRecordDO;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.ReviewItemDO;
import com.hewei.hzyjy.xunzhi.interview.dao.mapper.InterviewRecordMapper;
import com.hewei.hzyjy.xunzhi.interview.dao.mapper.ReviewItemMapper;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewRecordService;
import com.hewei.hzyjy.xunzhi.interview.service.ReviewItemService;
import com.hewei.hzyjy.xunzhi.knowledge.service.HybridSearchService;
import com.hewei.hzyjy.xunzhi.knowledge.service.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 复习清单服务：面试报告 → LLM 弱项抽取 → 复习条目落库（可选知识库关联）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewItemServiceImpl extends ServiceImpl<ReviewItemMapper, ReviewItemDO>
        implements ReviewItemService {

    private static final String EXTRACT_SYSTEM_PROMPT = """
            你是面试复习助手。根据面试反馈提取候选人的薄弱知识点。
            只输出 JSON 数组，不要任何解释或代码块标记，格式：
            [{"knowledgePoint":"知识点名称(不超过20字)","severity":2,"suggestion":"针对性复习建议(不超过200字)"}]
            severity 取 1(轻微)/2(一般)/3(严重)。提取 3~8 条，按严重度从高到低排列。""";

    private final InterviewRecordMapper interviewRecordMapper;
    private final InterviewRecordService interviewRecordService;
    private final QueryRewriteService queryRewriteService;
    private final HybridSearchService hybridSearchService;
    private final ReviewProperties reviewProperties;
    private final UserModelPreferenceService userModelPreferenceService;
    private final UniversalAiChatHandler universalAiChatHandler;

    @Override
    public ReviewGenerateRespDTO generateFromInterview(String sessionId, Long userId, String username, Long kbId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException("sessionId不能为空");
        }
        InterviewRecordDO record = interviewRecordMapper.selectOne(Wrappers.lambdaQuery(InterviewRecordDO.class)
                .eq(InterviewRecordDO::getUserId, userId)
                .eq(InterviewRecordDO::getSessionId, sessionId)
                .eq(InterviewRecordDO::getDelFlag, 0));
        if (record == null) {
            throw new ClientException("面试记录不存在");
        }
        if (!"FINISHED".equals(record.getInterviewStatus()) && !"EVALUATED".equals(record.getInterviewStatus())) {
            throw new ClientException("面试尚未结束，无法生成复习清单");
        }

        // 幂等：该会话已生成过则直接返回；若本次指定了知识库，为无参考的存量条目补充检索
        Long existing = baseMapper.selectCount(Wrappers.lambdaQuery(ReviewItemDO.class)
                .eq(ReviewItemDO::getSessionId, sessionId)
                .eq(ReviewItemDO::getDelFlag, 0));
        if (existing != null && existing > 0) {
            int backfilled = kbId == null ? 0 : backfillKbRefs(sessionId, userId, kbId);
            return new ReviewGenerateRespDTO(false, existing.intValue(), backfilled);
        }

        String llmOutput = extractWithTimeout(username, buildExtractUserPrompt(record,
                interviewRecordService.getBySessionId(sessionId, userId)));
        List<ReviewItemDO> items = parseWeakItems(llmOutput, reviewProperties.getMaxItemsPerSession());
        if (items.isEmpty()) {
            throw new ServiceException("弱项抽取失败，请稍后重试");
        }

        Date now = new Date();
        int inserted = 0;
        for (ReviewItemDO item : items) {
            item.setUserId(userId);
            item.setSessionId(sessionId);
            item.setStatus(0);
            item.setDelFlag(0);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (kbId != null) {
                item.setKbRefsJson(searchKbRefs(kbId, item.getKnowledgePoint()));
            }
            try {
                baseMapper.insert(item);
                inserted++;
            } catch (DuplicateKeyException e) {
                // 并发生成时唯一键兜底，忽略重复条目
                log.warn("Duplicate review item ignored, sessionId={}, point={}", sessionId, item.getKnowledgePoint());
            }
        }
        return new ReviewGenerateRespDTO(true, inserted, 0);
    }

    /**
     * 为已生成但缺参考片段的存量条目补跑知识库检索（fail-open），返回实际补上的条数。
     */
    private int backfillKbRefs(String sessionId, Long userId, Long kbId) {
        List<ReviewItemDO> pending = baseMapper.selectList(Wrappers.lambdaQuery(ReviewItemDO.class)
                .eq(ReviewItemDO::getUserId, userId)
                .eq(ReviewItemDO::getSessionId, sessionId)
                .eq(ReviewItemDO::getDelFlag, 0)
                .and(w -> w.isNull(ReviewItemDO::getKbRefsJson).or().eq(ReviewItemDO::getKbRefsJson, "")));
        int backfilled = 0;
        for (ReviewItemDO item : pending) {
            String refsJson = searchKbRefs(kbId, item.getKnowledgePoint());
            if (refsJson == null) {
                continue;
            }
            ReviewItemDO update = new ReviewItemDO();
            update.setId(item.getId());
            update.setKbRefsJson(refsJson);
            update.setUpdateTime(new Date());
            baseMapper.updateById(update);
            backfilled++;
        }
        return backfilled;
    }

    /**
     * LLM 抽取输入：面试方向/得分/总体建议 + 逐题反馈摘要。
     */
    private String buildExtractUserPrompt(InterviewRecordDO record, InterviewRecordRespDTO detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("面试方向：").append(StrUtil.blankToDefault(record.getInterviewDirection(), "未知")).append('\n');
        if (record.getInterviewScore() != null) {
            sb.append("面试总分：").append(record.getInterviewScore()).append('\n');
        }
        if (StrUtil.isNotBlank(record.getInterviewSuggestions())) {
            sb.append("总体建议：").append(StrUtil.maxLength(record.getInterviewSuggestions(), 1000)).append('\n');
        }
        if (detail != null && detail.getPlaybackItems() != null) {
            sb.append("逐题反馈：\n");
            for (InterviewPlaybackItemRespDTO item : detail.getPlaybackItems()) {
                sb.append("- 题目：").append(StrUtil.maxLength(StrUtil.nullToEmpty(item.getQuestionContent()), 100));
                if (item.getScore() != null) {
                    sb.append("｜得分：").append(item.getScore());
                }
                if (StrUtil.isNotBlank(item.getFeedback())) {
                    sb.append("｜反馈：").append(StrUtil.maxLength(item.getFeedback(), 200));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 带超时的 LLM 弱项抽取，失败/超时返回 null。
     * 优先用户绑定的复习模型，未绑定或调用失败时回退平台轻量模型非流式通道。
     */
    private String extractWithTimeout(String username, String userPrompt) {
        try {
            return CompletableFuture.supplyAsync(() -> extractOnce(username, userPrompt))
                    .get(reviewProperties.getExtractTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Review weak-point extraction failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractOnce(String username, String userPrompt) {
        AiPropertiesDO preferred = userModelPreferenceService.resolvePreferred(
                username, UserModelPreferenceService.FEATURE_REVIEW);
        if (preferred != null) {
            try {
                String output = universalAiChatHandler.completeOnce(preferred, EXTRACT_SYSTEM_PROMPT, userPrompt);
                if (StrUtil.isNotBlank(output)) {
                    return output;
                }
            } catch (Exception e) {
                // fail-open：用户模型故障不阻断复习生成，回退平台通道
                log.warn("Review extraction via user model failed, fallback to platform, username={}: {}",
                        username, e.getMessage());
            }
        }
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", EXTRACT_SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt));
        return queryRewriteService.complete(messages);
    }

    /**
     * 解析 LLM 输出为复习条目：非法/解析失败返回空列表（不写库）；
     * 去重同名知识点、severity 钳制 1~3、条数截断 maxItems。
     */
    static List<ReviewItemDO> parseWeakItems(String llmOutput, int maxItems) {
        List<ReviewItemDO> items = new ArrayList<>();
        if (StrUtil.isBlank(llmOutput)) {
            return items;
        }
        // 剥离可能的 markdown 代码块包裹
        String json = llmOutput.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```\\w*\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        JSONArray array;
        try {
            array = JSON.parseArray(json);
        } catch (Exception e) {
            return items;
        }
        if (array == null) {
            return items;
        }
        Set<String> seenPoints = new LinkedHashSet<>();
        for (int i = 0; i < array.size() && items.size() < maxItems; i++) {
            JSONObject obj = array.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            String point = StrUtil.trimToNull(obj.getString("knowledgePoint"));
            if (point == null || !seenPoints.add(point)) {
                continue;
            }
            ReviewItemDO item = new ReviewItemDO();
            item.setKnowledgePoint(StrUtil.maxLength(point, 120));
            Integer severity = obj.getInteger("severity");
            item.setSeverity(severity == null ? 2 : Math.max(1, Math.min(3, severity)));
            item.setSuggestion(StrUtil.maxLength(StrUtil.nullToEmpty(obj.getString("suggestion")), 1000));
            items.add(item);
        }
        return items;
    }

    /**
     * 知识库关联检索（fail-open）：命中片段序列化为 JSON，失败返回 null。
     */
    private String searchKbRefs(Long kbId, String knowledgePoint) {
        try {
            List<Map<String, Object>> hits = hybridSearchService.search(kbId, knowledgePoint, 3, 2);
            if (hits == null || hits.isEmpty()) {
                return null;
            }
            List<ReviewKbRefDTO> refs = new ArrayList<>();
            for (Map<String, Object> hit : hits) {
                ReviewKbRefDTO ref = new ReviewKbRefDTO();
                ref.setFileName(Objects.toString(hit.getOrDefault("file_name", ""), ""));
                ref.setDocId(Objects.toString(hit.getOrDefault("doc_id", ""), ""));
                String content = Objects.toString(hit.getOrDefault("content", ""), "");
                ref.setSnippet(content.length() > 200 ? content.substring(0, 200) : content);
                refs.add(ref);
            }
            return JSON.toJSONString(refs);
        } catch (Exception e) {
            log.warn("Review kb-ref search failed, kbId={}, point={}: {}", kbId, knowledgePoint, e.getMessage());
            return null;
        }
    }

    @Override
    public IPage<ReviewItemRespDTO> pageReviewItems(Long userId, ReviewItemPageReqDTO requestParam) {
        LambdaQueryWrapper<ReviewItemDO> wrapper = Wrappers.lambdaQuery(ReviewItemDO.class)
                .eq(ReviewItemDO::getUserId, userId)
                .eq(ReviewItemDO::getDelFlag, 0)
                .eq(requestParam.getStatus() != null, ReviewItemDO::getStatus, requestParam.getStatus())
                .orderByDesc(ReviewItemDO::getSeverity)
                .orderByDesc(ReviewItemDO::getCreateTime);
        Page<ReviewItemDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());
        return baseMapper.selectPage(page, wrapper).convert(ReviewItemServiceImpl::toRespDTO);
    }

    private static ReviewItemRespDTO toRespDTO(ReviewItemDO item) {
        ReviewItemRespDTO resp = new ReviewItemRespDTO();
        BeanUtils.copyProperties(item, resp);
        if (StrUtil.isNotBlank(item.getKbRefsJson())) {
            try {
                resp.setKbRefs(JSON.parseArray(item.getKbRefsJson(), ReviewKbRefDTO.class));
            } catch (Exception e) {
                // 历史脏数据容忍：引用解析失败不影响条目展示
            }
        }
        return resp;
    }

    @Override
    public void updateStatus(Long userId, Long itemId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ClientException("status非法");
        }
        ReviewItemDO item = baseMapper.selectById(itemId);
        if (item == null || item.getDelFlag() != 0 || !Objects.equals(item.getUserId(), userId)) {
            throw new ClientException("复习条目不存在");
        }
        ReviewItemDO update = new ReviewItemDO();
        update.setId(itemId);
        update.setStatus(status);
        update.setUpdateTime(new Date());
        baseMapper.updateById(update);
    }

    @Override
    public ReviewGrowthRespDTO growth(Long userId) {
        List<InterviewRecordDO> records = interviewRecordMapper.selectList(
                Wrappers.lambdaQuery(InterviewRecordDO.class)
                        .eq(InterviewRecordDO::getUserId, userId)
                        .eq(InterviewRecordDO::getDelFlag, 0)
                        .in(InterviewRecordDO::getInterviewStatus, List.of("FINISHED", "EVALUATED"))
                        .orderByAsc(InterviewRecordDO::getStartTime));
        List<ReviewGrowthRespDTO.ScorePointDTO> scoreTrend = records.stream()
                .map(r -> new ReviewGrowthRespDTO.ScorePointDTO(
                        r.getSessionId(), r.getInterviewScore(), r.getResumeScore(), r.getStartTime()))
                .toList();

        List<ReviewItemDO> items = baseMapper.selectList(Wrappers.lambdaQuery(ReviewItemDO.class)
                .eq(ReviewItemDO::getUserId, userId)
                .eq(ReviewItemDO::getDelFlag, 0));

        ReviewGrowthRespDTO resp = new ReviewGrowthRespDTO();
        resp.setScoreTrend(scoreTrend);
        resp.setWeakPoints(aggregateWeakPoints(items));
        return resp;
    }

    /**
     * 按知识点聚合：次数/掌握数/掌握率；顽固弱项（出现>=2 次）置顶，次数降序。
     */
    static List<ReviewGrowthRespDTO.WeakPointAggDTO> aggregateWeakPoints(List<ReviewItemDO> items) {
        Map<String, List<ReviewItemDO>> grouped = new LinkedHashMap<>();
        for (ReviewItemDO item : items) {
            grouped.computeIfAbsent(item.getKnowledgePoint(), k -> new ArrayList<>()).add(item);
        }
        List<ReviewGrowthRespDTO.WeakPointAggDTO> aggs = new ArrayList<>();
        for (Map.Entry<String, List<ReviewItemDO>> entry : grouped.entrySet()) {
            int total = entry.getValue().size();
            int mastered = (int) entry.getValue().stream()
                    .filter(i -> Integer.valueOf(1).equals(i.getStatus())).count();
            double masteryRate = Math.round((double) mastered / total * 10000) / 10000.0;
            aggs.add(new ReviewGrowthRespDTO.WeakPointAggDTO(
                    entry.getKey(), total, mastered, masteryRate, total >= 2));
        }
        aggs.sort(Comparator.comparing(ReviewGrowthRespDTO.WeakPointAggDTO::getStubborn).reversed()
                .thenComparing(Comparator.comparing(ReviewGrowthRespDTO.WeakPointAggDTO::getOccurrences).reversed()));
        return aggs;
    }
}
