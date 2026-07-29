package com.hewei.hzyjy.xunzhi.knowledge.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "xunzhi-agent.rag.vector-store", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchVectorStore implements VectorStore {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index-prefix:xunzhi_kb_}")
    private String indexPrefix;

    @Override
    public String getIndexName(Long kbId) {
        return indexPrefix + kbId;
    }

    @Override
    public void createIndexIfNotExists(Long kbId) {
        String indexName = getIndexName(kbId);
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
            if (esClient.indices().exists(existsRequest).value()) {
                return;
            }

            try {
                esClient.indices().create(buildCreateRequest(indexName, true));
            } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
                // 官方 ES 镜像未预装 IK 插件时回退 standard 分词，保证建库可用（中文分词质量会降级）
                if (e.getMessage() != null && e.getMessage().contains("analyzer")) {
                    log.warn("IK analyzer not available, falling back to standard analyzer for index {}. " +
                            "Install the analysis-ik plugin for better Chinese tokenization.", indexName);
                    esClient.indices().create(buildCreateRequest(indexName, false));
                } else {
                    throw e;
                }
            }
            log.info("Created ES index: {}", indexName);
        } catch (IOException e) {
            log.error("Failed to create ES index: {}", indexName, e);
            throw new RuntimeException("Failed to create ES index", e);
        }
    }

    private CreateIndexRequest buildCreateRequest(String indexName, boolean useIk) {
        DenseVectorProperty denseVectorProperty = DenseVectorProperty.of(d -> d
                .dims(1536)
                .similarity(DenseVectorSimilarity.Cosine)
                .index(true)
        );
        Map<String, Property> properties = new java.util.LinkedHashMap<>();
        properties.put("chunk_id", Property.of(p -> p.keyword(k -> k)));
        properties.put("doc_id", Property.of(p -> p.keyword(k -> k)));
        properties.put("kb_id", Property.of(p -> p.long_(k -> k)));
        properties.put("content", useIk
                ? Property.of(p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                : Property.of(p -> p.text(t -> t)));
        properties.put("file_name", Property.of(p -> p.keyword(k -> k)));
        properties.put("chunk_index", Property.of(p -> p.integer(k -> k)));
        properties.put("embedding", Property.of(p -> p.denseVector(denseVectorProperty)));
        // 父子分块：父块内容仅存储不参与检索（BM25 只查 content），命中后随 _source 回传
        properties.put("parent_id", Property.of(p -> p.keyword(k -> k)));
        properties.put("parent_content", Property.of(p -> p.text(t -> t.index(false))));
        properties.put("metadata", Property.of(p -> p.object(o -> o.enabled(true))));

        return CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(TypeMapping.of(m -> m.properties(properties)))
        );
    }

    @Override
    public void indexChunks(Long kbId, List<Map<String, Object>> chunks) {
        String indexName = getIndexName(kbId);
        createIndexIfNotExists(kbId);
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Map<String, Object> chunk : chunks) {
                bulkBuilder.operations(op -> op.index(idx -> idx
                        .index(indexName)
                        .id((String) chunk.get("chunk_id"))
                        .document(chunk)
                ));
            }
            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                log.warn("Bulk indexing had errors for index {}", indexName);
            }
            log.info("Indexed {} chunks to {}", chunks.size(), indexName);
        } catch (IOException e) {
            log.error("Failed to bulk index chunks to {}", indexName, e);
            throw new RuntimeException("Failed to index chunks", e);
        }
    }

    /**
     * msearch 并行双路召回：BM25 路 + kNN 路，两路均排除 embedding 回传，
     * 各自按 ES 得分降序，供客户端按排名做标准 RRF 融合。
     */
    @Override
    public DualRecallResult dualRecall(Long kbId, String query, List<Float> queryEmbedding, int candidateSize) {
        String indexName = getIndexName(kbId);
        try {
            RequestItem bm25Route = RequestItem.of(r -> r
                    .header(h -> h.index(indexName))
                    .body(b -> b
                            .query(q -> q
                                    .bool(bl -> bl
                                            .should(s1 -> s1.match(m -> m.field("content").query(query).boost(0.3f)))
                                            .should(s2 -> s2.match(m -> m.field("file_name").query(query).boost(0.1f)))
                                    )
                            )
                            .size(candidateSize)
                            .source(sc -> sc.filter(f -> f.excludes("embedding")))
                    )
            );
            RequestItem knnRoute = RequestItem.of(r -> r
                    .header(h -> h.index(indexName))
                    .body(b -> b
                            .knn(knn -> knn
                                    .field("embedding")
                                    .queryVector(queryEmbedding)
                                    .k(candidateSize)
                                    .numCandidates(candidateSize * 2)
                            )
                            .size(candidateSize)
                            .source(sc -> sc.filter(f -> f.excludes("embedding")))
                    )
            );

            MsearchRequest request = MsearchRequest.of(m -> m.searches(bm25Route).searches(knnRoute));
            MsearchResponse<Map> response = esClient.msearch(request, Map.class);

            List<Map<String, Object>> bm25Hits = extractHits(response.responses().get(0), indexName, "bm25");
            List<Map<String, Object>> knnHits = extractHits(response.responses().get(1), indexName, "knn");
            return new DualRecallResult(bm25Hits, knnHits);
        } catch (IOException e) {
            log.error("Dual recall msearch failed for index {}", indexName, e);
            throw new RuntimeException("Dual recall failed", e);
        }
    }

    private List<Map<String, Object>> extractHits(MultiSearchResponseItem<Map> item, String indexName, String route) {
        if (item.isFailure()) {
            log.warn("Dual recall {} route failed for index {}: {}", route, indexName, item.failure().error().reason());
            return List.of();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<Map> hit : item.result().hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                source.put("_score", hit.score() != null ? hit.score() : 0.0);
                results.add(source);
            }
        }
        return results;
    }

    /**
     * mget 一次批量取回 embedding（文档 _id 即 chunk_id），供 cosine 降级精排补查向量。
     */
    @Override
    public Map<String, List<Float>> fetchEmbeddings(Long kbId, List<String> chunkIds) {
        String indexName = getIndexName(kbId);
        Map<String, List<Float>> embeddings = new java.util.HashMap<>();
        if (chunkIds == null || chunkIds.isEmpty()) {
            return embeddings;
        }
        try {
            MgetRequest request = MgetRequest.of(m -> m
                    .index(indexName)
                    .ids(chunkIds)
                    .sourceIncludes("embedding")
            );
            MgetResponse<Map> response = esClient.mget(request, Map.class);
            for (MultiGetResponseItem<Map> item : response.docs()) {
                if (item.isResult() && item.result().found() && item.result().source() != null) {
                    Object embObj = item.result().source().get("embedding");
                    if (embObj instanceof List<?> embList && !embList.isEmpty()) {
                        List<Float> vector = new ArrayList<>();
                        for (Object val : embList) {
                            if (val instanceof Number num) {
                                vector.add(num.floatValue());
                            }
                        }
                        embeddings.put(item.result().id(), vector);
                    }
                }
            }
            return embeddings;
        } catch (IOException e) {
            log.error("Fetch embeddings mget failed for index {}", indexName, e);
            return embeddings;
        }
    }

    @Override
    public void deleteByDocId(Long kbId, String docId) {
        String indexName = getIndexName(kbId);
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("doc_id").value(docId)))
            );
            esClient.deleteByQuery(request);
            log.info("Deleted chunks for doc_id={} from index {}", docId, indexName);
        } catch (IOException e) {
            log.error("Failed to delete chunks for doc_id={} from {}", docId, indexName, e);
        }
    }

    @Override
    public void deleteIndex(Long kbId) {
        String indexName = getIndexName(kbId);
        try {
            esClient.indices().delete(de -> de.index(indexName));
            log.info("Deleted ES index: {}", indexName);
        } catch (IOException e) {
            log.error("Failed to delete ES index: {}", indexName, e);
        }
    }
}
