package com.hewei.hzyjy.xunzhi.knowledge.service;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.hewei.hzyjy.xunzhi.knowledge.dao.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEtlPipeline {

    // 父子分块（Small-to-Big）：父块负责上下文完整性，子块负责检索精度
    private static final int PARENT_CHUNK_SIZE = 1600;
    private static final int PARENT_OVERLAP_SIZE = 0;
    private static final int CHILD_CHUNK_SIZE = 400;
    private static final int CHILD_OVERLAP_SIZE = 50;

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeFileStore fileStore;

    /**
     * 异步处理文档。参数为 byte[] 而非 MultipartFile，避免主请求线程关闭后临时文件被清理导致读取失败。
     */
    @Async
    public void process(Long kbId, String username, byte[] fileBytes, String fileName) {
        process(kbId, username, fileBytes, fileName, null);
    }

    /**
     * 同步处理（不加 @Async，供索引重建串行复用）：existingDocId 非空时复用既有 Mongo 记录重灌索引，
     * 为空时新建文档并先留存原始文件。失败路径（解析失败/空文本/索引异常）将 status 置 3。
     *
     * @return 是否成功（status 置 2）
     */
    public boolean process(Long kbId, String username, byte[] fileBytes, String fileName, String existingDocId) {
        String fileType = getFileExtension(fileName);
        KnowledgeDocument doc;
        if (existingDocId == null) {
            String docId = UUID.randomUUID().toString();
            doc = new KnowledgeDocument();
            doc.setKbId(kbId);
            doc.setDocId(docId);
            doc.setFileName(fileName);
            doc.setFileType(fileType);
            doc.setFileSize((long) fileBytes.length);
            doc.setUsername(username);
            doc.setCreateTime(LocalDateTime.now());
            // 解析前先留存原始文件供索引重建复用（fail-open：落盘失败不阻断 ETL）
            doc.setFilePath(fileStore.save(kbId, docId, fileType, fileBytes));
        } else {
            doc = documentRepository.findByDocId(existingDocId).stream().findFirst().orElse(null);
            if (doc == null) {
                log.warn("Document not found for reprocess, docId={}", existingDocId);
                return false;
            }
        }
        doc.setStatus(1);
        doc.setUpdateTime(LocalDateTime.now());
        documentRepository.save(doc);

        String text;
        try {
            text = extractText(fileBytes, fileType);
        } catch (Exception e) {
            log.error("Failed to parse file: {}", fileName, e);
            markFailed(doc);
            return false;
        }
        if (StrUtil.isBlank(text)) {
            log.warn("Extracted text is empty for file: {}", fileName);
            markFailed(doc);
            return false;
        }

        try {
            // 两级分块：父块 1600/0 提供完整上下文，子块 400/50 用于向量检索；embedding 仅对子块计算
            List<String> parentChunks = chunkText(text, PARENT_CHUNK_SIZE, PARENT_OVERLAP_SIZE);
            List<Map<String, Object>> chunkDocs = new ArrayList<>();
            String docId = doc.getDocId();

            int childIndex = 0;
            for (int p = 0; p < parentChunks.size(); p++) {
                String parentContent = parentChunks.get(p);
                String parentId = docId + "_p" + p;
                for (String child : chunkText(parentContent, CHILD_CHUNK_SIZE, CHILD_OVERLAP_SIZE)) {
                    String chunkId = docId + "_" + childIndex;
                    List<Float> embedding = embeddingService.embed(child);

                    Map<String, Object> chunkDoc = new HashMap<>();
                    chunkDoc.put("chunk_id", chunkId);
                    chunkDoc.put("doc_id", docId);
                    chunkDoc.put("kb_id", kbId);
                    chunkDoc.put("content", child);
                    chunkDoc.put("file_name", fileName);
                    chunkDoc.put("chunk_index", childIndex);
                    chunkDoc.put("embedding", embedding.stream().map(Float::doubleValue).toList());
                    chunkDoc.put("parent_id", parentId);
                    chunkDoc.put("parent_content", parentContent);
                    chunkDoc.put("metadata", Map.of("char_count", child.length()));
                    chunkDocs.add(chunkDoc);
                    childIndex++;
                }
            }

            vectorStore.indexChunks(kbId, chunkDocs);

            doc.setChunkCount(chunkDocs.size());
            doc.setStatus(2);
            doc.setUpdateTime(LocalDateTime.now());
            documentRepository.save(doc);

            // 更新知识库的文档数和分块数
            updateKnowledgeBaseCounts(kbId);

            log.info("ETL pipeline completed, kbId={}, docId={}, parents={}, chunks={}", kbId, docId, parentChunks.size(), chunkDocs.size());
            return true;
        } catch (Exception e) {
            log.error("ETL indexing failed, kbId={}, docId={}", kbId, doc.getDocId(), e);
            markFailed(doc);
            return false;
        }
    }

    private void markFailed(KnowledgeDocument doc) {
        doc.setStatus(3);
        doc.setUpdateTime(LocalDateTime.now());
        documentRepository.save(doc);
        updateKnowledgeBaseCounts(doc.getKbId());
    }

    public void deleteDocument(Long kbId, String docId) {
        documentRepository.findByDocId(docId).stream().findFirst()
                .map(KnowledgeDocument::getFilePath)
                .ifPresent(fileStore::delete);
        vectorStore.deleteByDocId(kbId, docId);
        documentRepository.deleteByDocId(docId);
        updateKnowledgeBaseCounts(kbId);
    }

    void updateKnowledgeBaseCounts(Long kbId) {
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) return;
        List<KnowledgeDocument> docs = documentRepository.findByKbIdAndUsername(kbId, kb.getUsername());
        int docCount = docs.size();
        int chunkCount = docs.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 2)
                .mapToInt(d -> d.getChunkCount() != null ? d.getChunkCount() : 0)
                .sum();
        kb.setDocumentCount(docCount);
        kb.setChunkCount(chunkCount);
        kb.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);
    }

    /**
     * 根据文件类型解析文档内容，支持 txt/md/pdf/doc/docx。
     */
    private String extractText(byte[] fileBytes, String fileType) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
            return switch (fileType) {
                case "pdf" -> extractPdf(bais);
                case "doc" -> extractDoc(bais);
                case "docx" -> extractDocx(bais);
                default -> new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            };
        }
    }

    private String extractPdf(ByteArrayInputStream bais) throws IOException {
        try (PDDocument document = Loader.loadPDF(bais.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDoc(ByteArrayInputStream bais) throws IOException {
        try (HWPFDocument document = new HWPFDocument(bais);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocx(ByteArrayInputStream bais) throws IOException {
        try (XWPFDocument document = new XWPFDocument(bais)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 参数化分段切块：按空行分段聚合，超长段落按句末切分。
     * static 纯函数便于父/子两级复用与单测。
     */
    static List<String> chunkText(String text, int maxSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return chunks;
        }

        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (currentChunk.length() + trimmed.length() > maxSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                if (overlap > 0 && currentChunk.length() > overlap) {
                    String overlapText = currentChunk.substring(Math.max(0, currentChunk.length() - overlap));
                    currentChunk = new StringBuilder(overlapText);
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);

            while (currentChunk.length() > maxSize * 2) {
                int splitPos = maxSize;
                int sentenceEnd = findSentenceEnd(currentChunk.toString(), maxSize);
                if (sentenceEnd > 0 && sentenceEnd < maxSize + 200) {
                    splitPos = sentenceEnd;
                }
                chunks.add(currentChunk.substring(0, splitPos).trim());
                String remaining = currentChunk.substring(Math.max(0, splitPos - overlap));
                currentChunk = new StringBuilder(remaining);
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private static int findSentenceEnd(String text, int startPos) {
        for (int i = startPos + 200; i > startPos - 100 && i > 0; i--) {
            if (i < text.length()) {
                char c = text.charAt(i);
                if (c == '.' || c == '。' || c == '!' || c == '！' || c == '?' || c == '？' || c == '\n') {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    public static boolean isSupportedFileType(String fileName) {
        if (fileName == null) {
            return false;
        }
        String ext = fileName.toLowerCase();
        return ext.endsWith(".txt") || ext.endsWith(".md") || ext.endsWith(".pdf")
                || ext.endsWith(".doc") || ext.endsWith(".docx");
    }
}
