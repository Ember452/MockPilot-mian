package com.hewei.hzyjy.xunzhi.knowledge.api;

import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.knowledge.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RAG 观测看板接口：汇总统计与明细分页均基于 rag_trace 留档（按用户隔离）。
 */
@Slf4j
@RestController
@RequestMapping("/api/xunzhi/v1/rag/metrics")
@RequiredArgsConstructor
public class RagMetricsController {

    private final RagTraceService ragTraceService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam(defaultValue = "7") int days,
                                               @CurrentUser String username) {
        return Results.success(ragTraceService.summary(username, days));
    }

    @GetMapping("/traces")
    public Result<Map<String, Object>> traces(@RequestParam(required = false) Long kbId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @CurrentUser String username) {
        return Results.success(ragTraceService.traces(username, kbId, page, size));
    }
}
