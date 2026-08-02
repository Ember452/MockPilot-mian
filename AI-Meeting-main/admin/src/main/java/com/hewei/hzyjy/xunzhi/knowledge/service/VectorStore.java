package com.hewei.hzyjy.xunzhi.knowledge.service;

import java.util.List;
import java.util.Map;

/**
 * 向量存储抽象：默认 Elasticsearch 实现，可经
 * xunzhi-agent.rag.vector-store 配置切换（elasticsearch | milvus）。
 * 双路召回 + 客户端 RRF 融合的架构对引擎透明，实现只需各自返回
 * 关键词路与向量路的降序命中列表。
 */
public interface VectorStore {

    /**
     * 索引/集合命名（xunzhi_kb_{kbId}），两引擎保持一致。
     */
    String getIndexName(Long kbId);

    void createIndexIfNotExists(Long kbId);

    void indexChunks(Long kbId, List<Map<String, Object>> chunks);

    /**
     * 双路召回：关键词（BM25）路 + 向量（ANN）路，两路均按各自引擎得分降序，
     * 且不回传 embedding 字段，供客户端按排名做标准 RRF 融合。
     * TODO 当前只支持在一个知识库中进行双路召回。可考虑扩展为多个知识库进行双路召回。
     */
    DualRecallResult dualRecall(Long kbId, String query, List<Float> queryEmbedding, int candidateSize);

    /**
     * 按 chunk_id 一次批量取回 embedding，供 cosine 降级精排补查向量。
     */
    Map<String, List<Float>> fetchEmbeddings(Long kbId, List<String> chunkIds);

    void deleteByDocId(Long kbId, String docId);

    void deleteIndex(Long kbId);

    /**
     * 双路召回结果，两路均已按引擎得分降序。
     */
    record DualRecallResult(List<Map<String, Object>> bm25Hits, List<Map<String, Object>> knnHits) {
    }
}
