package com.hewei.hzyjy.xunzhi.knowledge.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * RAG 链路 Micrometer 埋点集中入口：节点只调本组件，所有记录失败静默——
 * 观测不得影响主链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagMetricsRecorder {

    private final MeterRegistry meterRegistry;

    /** 阶段耗时分布：xunzhi_rag_stage_seconds{stage} */
    public void recordStage(String stage, long elapsedMillis) {
        try {
            Timer.builder("xunzhi_rag_stage_seconds")
                    .tag("stage", stage)
                    .register(meterRegistry)
                    .record(elapsedMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Record rag stage metric failed: {}", e.getMessage());
        }
    }

    /** 改写结果：xunzhi_rag_rewrite_total{result=applied|skipped|failed} */
    public void recordRewrite(String result) {
        increment("xunzhi_rag_rewrite_total", "result", result);
    }

    /** 精排通道：xunzhi_rag_rerank_total{provider=dashscope|cosine} */
    public void recordRerank(String provider) {
        increment("xunzhi_rag_rerank_total", "provider", provider);
    }

    /** 检索评估判定：xunzhi_rag_grader_total{decision=pass|web_search} */
    public void recordGrader(String decision) {
        increment("xunzhi_rag_grader_total", "decision", decision);
    }

    private void increment(String name, String tagKey, String tagValue) {
        try {
            Counter.builder(name)
                    .tag(tagKey, tagValue == null ? "unknown" : tagValue)
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.debug("Record rag counter failed: {}", e.getMessage());
        }
    }
}
