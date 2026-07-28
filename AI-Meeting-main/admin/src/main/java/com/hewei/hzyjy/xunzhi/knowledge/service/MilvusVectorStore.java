package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.SearchResp;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 2.5 实现：dense 路走 FloatVector ANN，关键词路走 BM25 Function
 * 生成的 sparse 向量全文检索，双路各自降序返回，客户端 RRF/rerank 链路零改动复用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "xunzhi-agent.rag.vector-store", havingValue = "milvus")
public class MilvusVectorStore implements VectorStore {

    private static final int EMBEDDING_DIM = 1536;
    private static final List<String> OUTPUT_FIELDS = List.of(
            "chunk_id", "doc_id", "kb_id", "content", "file_name",
            "chunk_index", "parent_id", "parent_content", "metadata");

    private final MilvusClientV2 milvusClient;
    private final Gson gson = new Gson();

    @Value("${milvus.collection-prefix:xunzhi_kb_}")
    private String collectionPrefix;

    @Override
    public String getIndexName(Long kbId) {
        return collectionPrefix + kbId;
    }

    @Override
    public void createIndexIfNotExists(Long kbId) {
        String collectionName = getIndexName(kbId);
        try {
            if (milvusClient.hasCollection(HasCollectionReq.builder()
                    .collectionName(collectionName).build())) {
                return;
            }

            CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();
            schema.addField(AddFieldReq.builder().fieldName("chunk_id")
                    .dataType(DataType.VarChar).maxLength(256).isPrimaryKey(true).autoID(false).build());
            schema.addField(AddFieldReq.builder().fieldName("doc_id")
                    .dataType(DataType.VarChar).maxLength(128).build());
            schema.addField(AddFieldReq.builder().fieldName("kb_id")
                    .dataType(DataType.Int64).build());
            // 中文分析器 + BM25 Function 生成 sparse 向量，实现引擎内全文检索
            schema.addField(AddFieldReq.builder().fieldName("content")
                    .dataType(DataType.VarChar).maxLength(65535)
                    .enableAnalyzer(true).analyzerParams(Map.of("type", "chinese")).build());
            schema.addField(AddFieldReq.builder().fieldName("file_name")
                    .dataType(DataType.VarChar).maxLength(512).build());
            schema.addField(AddFieldReq.builder().fieldName("chunk_index")
                    .dataType(DataType.Int32).build());
            schema.addField(AddFieldReq.builder().fieldName("embedding")
                    .dataType(DataType.FloatVector).dimension(EMBEDDING_DIM).build());
            schema.addField(AddFieldReq.builder().fieldName("parent_id")
                    .dataType(DataType.VarChar).maxLength(256).build());
            schema.addField(AddFieldReq.builder().fieldName("parent_content")
                    .dataType(DataType.VarChar).maxLength(65535).build());
            schema.addField(AddFieldReq.builder().fieldName("metadata")
                    .dataType(DataType.JSON).build());
            schema.addField(AddFieldReq.builder().fieldName("sparse")
                    .dataType(DataType.SparseFloatVector).build());
            schema.addFunction(CreateCollectionReq.Function.builder()
                    .functionType(FunctionType.BM25)
                    .name("content_bm25")
                    .inputFieldNames(Collections.singletonList("content"))
                    .outputFieldNames(Collections.singletonList("sparse"))
                    .build());

            List<IndexParam> indexParams = new ArrayList<>();
            indexParams.add(IndexParam.builder().fieldName("embedding")
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.COSINE).build());
            indexParams.add(IndexParam.builder().fieldName("sparse")
                    .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX)
                    .metricType(IndexParam.MetricType.BM25).build());

            milvusClient.createCollection(CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexParams)
                    .build());
            log.info("Created Milvus collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to create Milvus collection: {}", collectionName, e);
            throw new RuntimeException("Failed to create Milvus collection", e);
        }
    }

    @Override
    public void indexChunks(Long kbId, List<Map<String, Object>> chunks) {
        String collectionName = getIndexName(kbId);
        createIndexIfNotExists(kbId);
        try {
            List<JsonObject> rows = new ArrayList<>(chunks.size());
            for (Map<String, Object> chunk : chunks) {
                rows.add(gson.toJsonTree(chunk).getAsJsonObject());
            }
            milvusClient.insert(InsertReq.builder()
                    .collectionName(collectionName)
                    .data(rows)
                    .build());
            log.info("Indexed {} chunks to {}", chunks.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to insert chunks to {}", collectionName, e);
            throw new RuntimeException("Failed to index chunks", e);
        }
    }

    /**
     * 两次独立 search：sparse BM25 全文检索路 + dense ANN 路，
     * 各自按引擎得分降序且不回传 embedding，供客户端标准 RRF 融合。
     */
    @Override
    public DualRecallResult dualRecall(Long kbId, String query, List<Float> queryEmbedding, int candidateSize) {
        String collectionName = getIndexName(kbId);
        try {
            SearchResp bm25Resp = milvusClient.search(SearchReq.builder()
                    .collectionName(collectionName)
                    .annsField("sparse")
                    .data(Collections.singletonList(new EmbeddedText(query)))
                    .topK(candidateSize)
                    .outputFields(OUTPUT_FIELDS)
                    .build());
            SearchResp knnResp = milvusClient.search(SearchReq.builder()
                    .collectionName(collectionName)
                    .annsField("embedding")
                    .data(Collections.singletonList(new FloatVec(queryEmbedding)))
                    .topK(candidateSize)
                    .outputFields(OUTPUT_FIELDS)
                    .build());
            return new DualRecallResult(extractHits(bm25Resp), extractHits(knnResp));
        } catch (Exception e) {
            log.error("Dual recall failed for collection {}", collectionName, e);
            throw new RuntimeException("Dual recall failed", e);
        }
    }

    private List<Map<String, Object>> extractHits(SearchResp resp) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (resp == null || resp.getSearchResults() == null || resp.getSearchResults().isEmpty()) {
            return results;
        }
        for (SearchResp.SearchResult result : resp.getSearchResults().get(0)) {
            Map<String, Object> hit = new HashMap<>(result.getEntity());
            hit.put("_score", result.getScore() != null ? result.getScore().doubleValue() : 0.0);
            results.add(hit);
        }
        return results;
    }

    /**
     * get by ids 批量取回 embedding，供 cosine 降级精排补查向量。
     */
    @Override
    public Map<String, List<Float>> fetchEmbeddings(Long kbId, List<String> chunkIds) {
        String collectionName = getIndexName(kbId);
        Map<String, List<Float>> embeddings = new HashMap<>();
        if (chunkIds == null || chunkIds.isEmpty()) {
            return embeddings;
        }
        try {
            GetResp resp = milvusClient.get(GetReq.builder()
                    .collectionName(collectionName)
                    .ids(new ArrayList<>(chunkIds))
                    .outputFields(List.of("chunk_id", "embedding"))
                    .build());
            for (QueryResp.QueryResult result : resp.getGetResults()) {
                Object chunkId = result.getEntity().get("chunk_id");
                Object embObj = result.getEntity().get("embedding");
                if (chunkId != null && embObj instanceof List<?> embList && !embList.isEmpty()) {
                    List<Float> vector = new ArrayList<>();
                    for (Object val : embList) {
                        if (val instanceof Number num) {
                            vector.add(num.floatValue());
                        }
                    }
                    embeddings.put(chunkId.toString(), vector);
                }
            }
            return embeddings;
        } catch (Exception e) {
            log.error("Fetch embeddings failed for collection {}", collectionName, e);
            return embeddings;
        }
    }

    @Override
    public void deleteByDocId(Long kbId, String docId) {
        String collectionName = getIndexName(kbId);
        try {
            milvusClient.delete(DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter("doc_id == \"" + docId + "\"")
                    .build());
            log.info("Deleted chunks for doc_id={} from collection {}", docId, collectionName);
        } catch (Exception e) {
            log.error("Failed to delete chunks for doc_id={} from {}", docId, collectionName, e);
        }
    }

    @Override
    public void deleteIndex(Long kbId) {
        String collectionName = getIndexName(kbId);
        try {
            milvusClient.dropCollection(DropCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
            log.info("Dropped Milvus collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to drop Milvus collection: {}", collectionName, e);
        }
    }
}
