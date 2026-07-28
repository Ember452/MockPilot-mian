package com.hewei.hzyjy.xunzhi.interview.service.impl;

import com.hewei.hzyjy.xunzhi.ai.service.UserModelPreferenceService;
import com.hewei.hzyjy.xunzhi.ai.service.chat.UniversalAiChatHandler;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ServiceException;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGenerateRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGrowthRespDTO;
import com.hewei.hzyjy.xunzhi.interview.config.ReviewProperties;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewRecordDO;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.ReviewItemDO;
import com.hewei.hzyjy.xunzhi.interview.dao.mapper.InterviewRecordMapper;
import com.hewei.hzyjy.xunzhi.interview.dao.mapper.ReviewItemMapper;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewRecordService;
import com.hewei.hzyjy.xunzhi.knowledge.service.HybridSearchService;
import com.hewei.hzyjy.xunzhi.knowledge.service.QueryRewriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 复习清单服务单测：生成幂等、LLM 解析失败无脏数据、弱项聚合正确。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewItemServiceImplTest {

    @Mock
    private InterviewRecordMapper interviewRecordMapper;
    @Mock
    private InterviewRecordService interviewRecordService;
    @Mock
    private QueryRewriteService queryRewriteService;
    @Mock
    private HybridSearchService hybridSearchService;
    @Mock
    private ReviewItemMapper reviewItemMapper;
    @Mock
    private UserModelPreferenceService userModelPreferenceService;
    @Mock
    private UniversalAiChatHandler universalAiChatHandler;

    private ReviewItemServiceImpl service;

    @BeforeEach
    void setUp() {
        // resolvePreferred 默认返回 null（Mockito 缺省），即未绑定复习模型，走平台通道
        service = new ReviewItemServiceImpl(interviewRecordMapper, interviewRecordService,
                queryRewriteService, hybridSearchService, new ReviewProperties(),
                userModelPreferenceService, universalAiChatHandler);
        // ServiceImpl.baseMapper 由 Spring 注入，纯 Mockito 场景下反射写入
        ReflectionTestUtils.setField(service, "baseMapper", reviewItemMapper);
    }

    private InterviewRecordDO finishedRecord() {
        InterviewRecordDO record = new InterviewRecordDO();
        record.setUserId(1L);
        record.setSessionId("s-1");
        record.setInterviewStatus("FINISHED");
        record.setInterviewDirection("Java 后端");
        record.setInterviewScore(72);
        return record;
    }

    // ---------- generateFromInterview ----------

    @Test
    void generateShouldRejectMissingRecord() {
        when(interviewRecordMapper.selectOne(any())).thenReturn(null);

        assertThrows(ClientException.class,
                () -> service.generateFromInterview("s-x", 1L, "u-1", null));
    }

    @Test
    void generateShouldRejectUnfinishedInterview() {
        InterviewRecordDO record = finishedRecord();
        record.setInterviewStatus("IN_PROGRESS");
        when(interviewRecordMapper.selectOne(any())).thenReturn(record);

        assertThrows(ClientException.class,
                () -> service.generateFromInterview("s-1", 1L, "u-1", null));
    }

    @Test
    void generateShouldBeIdempotentWhenItemsExist() {
        when(interviewRecordMapper.selectOne(any())).thenReturn(finishedRecord());
        when(reviewItemMapper.selectCount(any())).thenReturn(4L);

        ReviewGenerateRespDTO resp = service.generateFromInterview("s-1", 1L, "u-1", null);

        assertFalse(resp.getGenerated());
        assertEquals(4, resp.getItemCount());
        assertEquals(0, resp.getRefsBackfilled());
        verify(queryRewriteService, never()).complete(anyList());
        verify(reviewItemMapper, never()).insert(any(ReviewItemDO.class));
        // kbId 为空时不触发补充检索
        verify(reviewItemMapper, never()).selectList(any());
    }

    @Test
    void generateShouldBackfillRefsWhenIdempotentWithKbId() {
        when(interviewRecordMapper.selectOne(any())).thenReturn(finishedRecord());
        when(reviewItemMapper.selectCount(any())).thenReturn(2L);
        ReviewItemDO noRef = new ReviewItemDO();
        noRef.setId(11L);
        noRef.setKnowledgePoint("JVM 内存模型");
        when(reviewItemMapper.selectList(any())).thenReturn(List.of(noRef));
        when(hybridSearchService.search(org.mockito.ArgumentMatchers.eq(7L), any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(java.util.Map.of(
                        "file_name", "jvm.md", "doc_id", "d-1", "content", "堆栈划分")));
        when(reviewItemMapper.updateById(any(ReviewItemDO.class))).thenReturn(1);

        ReviewGenerateRespDTO resp = service.generateFromInterview("s-1", 1L, "u-1", 7L);

        assertFalse(resp.getGenerated());
        assertEquals(1, resp.getRefsBackfilled());
        verify(reviewItemMapper).updateById(any(ReviewItemDO.class));
        verify(queryRewriteService, never()).complete(anyList());
    }

    @Test
    void generateShouldTolerateBackfillSearchFailure() {
        when(interviewRecordMapper.selectOne(any())).thenReturn(finishedRecord());
        when(reviewItemMapper.selectCount(any())).thenReturn(1L);
        ReviewItemDO noRef = new ReviewItemDO();
        noRef.setId(12L);
        noRef.setKnowledgePoint("索引优化");
        when(reviewItemMapper.selectList(any())).thenReturn(List.of(noRef));
        when(hybridSearchService.search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenThrow(new RuntimeException("es down"));

        ReviewGenerateRespDTO resp = service.generateFromInterview("s-1", 1L, "u-1", 7L);

        assertFalse(resp.getGenerated());
        assertEquals(0, resp.getRefsBackfilled());
        verify(reviewItemMapper, never()).updateById(any(ReviewItemDO.class));
    }

    @Test
    void generateShouldNotPersistWhenLlmOutputInvalid() {
        when(interviewRecordMapper.selectOne(any())).thenReturn(finishedRecord());
        when(reviewItemMapper.selectCount(any())).thenReturn(0L);
        when(queryRewriteService.complete(anyList())).thenReturn("对不起，我无法解析");

        assertThrows(ServiceException.class,
                () -> service.generateFromInterview("s-1", 1L, "u-1", null));
        verify(reviewItemMapper, never()).insert(any(ReviewItemDO.class));
    }

    @Test
    void generateShouldPersistParsedItems() {
        when(interviewRecordMapper.selectOne(any())).thenReturn(finishedRecord());
        when(reviewItemMapper.selectCount(any())).thenReturn(0L);
        when(reviewItemMapper.insert(any(ReviewItemDO.class))).thenReturn(1);
        when(queryRewriteService.complete(anyList())).thenReturn("""
                [{"knowledgePoint":"JVM 内存模型","severity":3,"suggestion":"复习堆栈划分"},
                 {"knowledgePoint":"索引优化","severity":2,"suggestion":"复习联合索引"}]""");

        ReviewGenerateRespDTO resp = service.generateFromInterview("s-1", 1L, "u-1", null);

        assertTrue(resp.getGenerated());
        assertEquals(2, resp.getItemCount());
        verify(reviewItemMapper, times(2)).insert(any(ReviewItemDO.class));
        verify(hybridSearchService, never()).search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    // ---------- parseWeakItems ----------

    @Test
    void parseShouldStripMarkdownFence() {
        List<ReviewItemDO> items = ReviewItemServiceImpl.parseWeakItems("""
                ```json
                [{"knowledgePoint":"TCP 握手","severity":1,"suggestion":"复习三次握手"}]
                ```""", 8);

        assertEquals(1, items.size());
        assertEquals("TCP 握手", items.get(0).getKnowledgePoint());
        assertEquals(1, items.get(0).getSeverity());
    }

    @Test
    void parseShouldReturnEmptyOnInvalidJson() {
        assertTrue(ReviewItemServiceImpl.parseWeakItems("not a json", 8).isEmpty());
        assertTrue(ReviewItemServiceImpl.parseWeakItems(null, 8).isEmpty());
        assertTrue(ReviewItemServiceImpl.parseWeakItems("  ", 8).isEmpty());
    }

    @Test
    void parseShouldDedupeClampAndTruncate() {
        List<ReviewItemDO> items = ReviewItemServiceImpl.parseWeakItems("""
                [{"knowledgePoint":"A","severity":9,"suggestion":"x"},
                 {"knowledgePoint":"A","severity":1,"suggestion":"dup"},
                 {"knowledgePoint":"B","severity":-1,"suggestion":"y"},
                 {"knowledgePoint":"C"},
                 {"knowledgePoint":"D","severity":2,"suggestion":"z"}]""", 3);

        assertEquals(3, items.size());
        assertEquals(3, items.get(0).getSeverity()); // 9 -> 3
        assertEquals(1, items.get(1).getSeverity()); // -1 -> 1
        assertEquals(2, items.get(2).getSeverity()); // 缺省 -> 2
    }

    // ---------- aggregateWeakPoints ----------

    @Test
    void aggregateShouldPutStubbornFirstAndComputeMastery() {
        ReviewItemDO a1 = item("Redis", 0);
        ReviewItemDO a2 = item("Redis", 1);
        ReviewItemDO b = item("MQ", 1);

        List<ReviewGrowthRespDTO.WeakPointAggDTO> aggs =
                ReviewItemServiceImpl.aggregateWeakPoints(List.of(b, a1, a2));

        assertEquals(2, aggs.size());
        assertEquals("Redis", aggs.get(0).getKnowledgePoint());
        assertTrue(aggs.get(0).getStubborn());
        assertEquals(2, aggs.get(0).getOccurrences());
        assertEquals(0.5, aggs.get(0).getMasteryRate());
        assertFalse(aggs.get(1).getStubborn());
        assertEquals(1.0, aggs.get(1).getMasteryRate());
    }

    @Test
    void aggregateShouldHandleEmptyList() {
        assertTrue(ReviewItemServiceImpl.aggregateWeakPoints(List.of()).isEmpty());
    }

    private ReviewItemDO item(String point, int status) {
        ReviewItemDO item = new ReviewItemDO();
        item.setKnowledgePoint(point);
        item.setStatus(status);
        return item;
    }

    // ---------- updateStatus ----------

    @Test
    void updateStatusShouldRejectForeignItem() {
        ReviewItemDO item = new ReviewItemDO();
        item.setId(9L);
        item.setUserId(2L);
        item.setDelFlag(0);
        when(reviewItemMapper.selectById(9L)).thenReturn(item);

        assertThrows(ClientException.class, () -> service.updateStatus(1L, 9L, 1));
        verify(reviewItemMapper, never()).updateById(any(ReviewItemDO.class));
    }

    @Test
    void updateStatusShouldPersistOwnItem() {
        ReviewItemDO item = new ReviewItemDO();
        item.setId(9L);
        item.setUserId(1L);
        item.setDelFlag(0);
        when(reviewItemMapper.selectById(9L)).thenReturn(item);
        when(reviewItemMapper.updateById(any(ReviewItemDO.class))).thenReturn(1);

        service.updateStatus(1L, 9L, 1);

        verify(reviewItemMapper).updateById(any(ReviewItemDO.class));
    }

    @Test
    void generateShouldTolerateNullDetail() {
        // getBySessionId 返回 null 时 prompt 仍可构建（仅少逐题反馈）
        when(interviewRecordMapper.selectOne(any())).thenReturn(finishedRecord());
        when(reviewItemMapper.selectCount(any())).thenReturn(0L);
        when(interviewRecordService.getBySessionId("s-1", 1L)).thenReturn(null);
        when(queryRewriteService.complete(anyList())).thenReturn(
                "[{\"knowledgePoint\":\"GC 调优\",\"severity\":2,\"suggestion\":\"复习分代回收\"}]");
        when(reviewItemMapper.insert(any(ReviewItemDO.class))).thenReturn(1);

        ReviewGenerateRespDTO resp = service.generateFromInterview("s-1", 1L, "u-1", null);

        assertTrue(resp.getGenerated());
        assertEquals(1, resp.getItemCount());
    }
}
