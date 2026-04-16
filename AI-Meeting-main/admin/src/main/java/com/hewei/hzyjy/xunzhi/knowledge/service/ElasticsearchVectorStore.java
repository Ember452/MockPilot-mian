package com.hewei.hzyjy.xunzhi.knowledge.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchVectorStore {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index-prefix:xunzhi_kb_}")
    private String indexPrefix;

    public String getIndexName(Long kbId) {
        return indexPrefix + kbId;
    }

    public void createIndexIfNotExists(Long kbId) {
        String indexName = getIndexName(kbId);
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
            if (esClient.indices().exists(existsRequest).value()) {
                return;
            }

            DenseVectorProperty denseVectorProperty = DenseVectorProperty.of(d -> d
                    .dims(1536)
                    .similarity(DenseVectorSimilarity.Cosine)
                    .index(true)
            );
            Map<String, Property> properties = new java.util.LinkedHashMap<>();
            properties.put("chunk_id", Property.of(p -> p.keyword(k -> k)));
            properties.put("doc_id", Property.of(p -> p.keyword(k -> k)));
            properties.put("kb_id", Property.of(p -> p.long_(k -> k)));
            properties.put("content", Property.of(p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart"))));
            properties.put("file_name", Property.of(p -> p.keyword(k -> k)));
            properties.put("chunk_index", Property.of(p -> p.integer(k -> k)));
            properties.put("embedding", Property.of(p -> p.denseVector(denseVectorProperty)));
            properties.put("metadata", Property.of(p -> p.object(o -> o.enabled(true))));

            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(TypeMapping.of(m -> m.properties(properties)))
            );
            esClient.indices().create(createRequest);
            log.info("Created ES index: {}", indexName);
        } catch (IOException e) {
            log.error("Failed to create ES index: {}", indexName, e);
            throw new RuntimeException("Failed to create ES index", e);
        }
    }

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

    public List<Map<String, Object>> hybridSearch(Long kbId, String query, List<Float> queryEmbedding, int topK) {
        String indexName = getIndexName(kbId);
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .knn(knn -> knn
                            .field("embedding")
                            .queryVector(queryEmbedding)
                            .k(topK * 2)
                            .numCandidates(topK * 4)
                    )
                    .query(q -> q
                            .bool(b -> b
                                    .should(s1 -> s1.match(m -> m.field("content").query(query).boost(0.3f)))
                                    .should(s2 -> s2.match(m -> m.field("file_name").query(query).boost(0.1f)))
                            )
                    )
                    .size(topK)
            );

            SearchResponse<Map> response = esClient.search(searchRequest, Map.class);

            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    source.put("_score", hit.score() != null ? hit.score() : 0.0);
                    results.add(source);
                }
            }

            return results;
        } catch (IOException e) {
            log.error("Hybrid search failed for index {}", indexName, e);
            throw new RuntimeException("Hybrid search failed", e);
        }
    }

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
