package com.hewei.hzyjy.xunzhi.knowledge.service;

import java.util.List;
import java.util.Map;

/**
 * 精排服务：对召回候选按与 query 的相关性重排，取 topN。
 * 实现需在候选上写入 _rerank_score 与 _rerank_provider 标记。
 */
public interface RerankService {

    List<Map<String, Object>> rerank(String query, List<Float> queryEmbedding,
                                     List<Map<String, Object>> candidates, int topN);
}
