package com.hewei.hzyjy.xunzhi.knowledge.service;

import cn.hutool.json.JSONUtil;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.hewei.hzyjy.xunzhi.knowledge.dao.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库索引重建：删索引 → 建索引 → 串行按留存原始文件重跑 ETL（复用原 docId）。
 * 锁用 Redis SETNX + TTL 而非 Redisson RLock——RLock 线程绑定，异步重建任务无法释放接口线程加的锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRebuildService {

    private static final String LOCK_KEY_PREFIX = "kb:rebuild:";
    private static final String PROGRESS_KEY_PREFIX = "kb:rebuild:progress:";
    /** 锁 TTL 兜底：重建进程崩溃时最长 1h 后自动解锁，重新触发即可自愈 */
    private static final Duration LOCK_TTL = Duration.ofHours(1);
    private static final Duration PROGRESS_TTL = Duration.ofHours(1);

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentEtlPipeline documentEtlPipeline;
    private final KnowledgeFileStore fileStore;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final StringRedisTemplate stringRedisTemplate;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 触发重建。force=false 时校验 embedding 兼容性；force=true 为换模型通道——
     * 重建前把库元数据更新为当前 embedding 配置（§8.5 预留口子）。
     */
    public void startRebuild(Long kbId, String username, boolean force) {
        KnowledgeBaseDO kb = knowledgeBaseService.getKnowledgeBase(kbId, username);
        if (!force) {
            knowledgeBaseService.ensureEmbeddingCompatible(kbId, username);
        }
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey(kbId), "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new ClientException("该知识库正在重建中，请稍后再试");
        }
        try {
            if (force) {
                kb.setEmbeddingModel(embeddingService.getEmbeddingModel());
                kb.setEmbeddingDim(embeddingService.getEmbeddingDimension());
                kb.setUpdateTime(LocalDateTime.now());
                knowledgeBaseMapper.updateById(kb);
                log.info("Rebuild with force: kb embedding updated, kbId={}, model={}", kbId, kb.getEmbeddingModel());
            }
            List<KnowledgeDocument> docs = documentRepository.findByKbIdAndUsername(kbId, username);
            writeProgress(kbId, docs.size(), 0, 0);
            threadPoolTaskExecutor.execute(() -> doRebuild(kbId, username, docs));
        } catch (Exception e) {
            stringRedisTemplate.delete(lockKey(kbId));
            throw e;
        }
    }

    public boolean isRebuilding(Long kbId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey(kbId)));
    }

    public Map<String, Object> getStatus(Long kbId, String username) {
        knowledgeBaseService.getKnowledgeBase(kbId, username);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("rebuilding", isRebuilding(kbId));
        String progress = stringRedisTemplate.opsForValue().get(progressKey(kbId));
        resp.put("progress", progress == null ? null : JSONUtil.parseObj(progress));
        return resp;
    }

    /**
     * 异步任务内串行重跑，避免并发 embed 触发限流与进度计数失真。
     */
    private void doRebuild(Long kbId, String username, List<KnowledgeDocument> docs) {
        try {
            vectorStore.deleteIndex(kbId);
            vectorStore.createIndexIfNotExists(kbId);
            int done = 0;
            int skipped = 0;
            for (KnowledgeDocument doc : docs) {
                byte[] fileBytes = fileStore.read(doc.getFilePath());
                if (fileBytes == null) {
                    // 无留存原始文件（存量文档）无法重建，标记失败并计入 skipped
                    doc.setStatus(3);
                    doc.setUpdateTime(LocalDateTime.now());
                    documentRepository.save(doc);
                    skipped++;
                } else {
                    boolean ok = documentEtlPipeline.process(kbId, username, fileBytes, doc.getFileName(), doc.getDocId());
                    if (ok) {
                        done++;
                    } else {
                        skipped++;
                    }
                }
                writeProgress(kbId, docs.size(), done, skipped);
            }
            documentEtlPipeline.updateKnowledgeBaseCounts(kbId);
            log.info("Rebuild completed, kbId={}, total={}, done={}, skipped={}", kbId, docs.size(), done, skipped);
        } catch (Exception e) {
            log.error("Rebuild failed, kbId={}", kbId, e);
        } finally {
            stringRedisTemplate.delete(lockKey(kbId));
        }
    }

    private void writeProgress(Long kbId, int total, int done, int skipped) {
        String json = String.format("{\"total\":%d,\"done\":%d,\"skipped\":%d}", total, done, skipped);
        stringRedisTemplate.opsForValue().set(progressKey(kbId), json, PROGRESS_TTL);
    }

    private String lockKey(Long kbId) {
        return LOCK_KEY_PREFIX + kbId;
    }

    private String progressKey(Long kbId) {
        return PROGRESS_KEY_PREFIX + kbId;
    }
}
