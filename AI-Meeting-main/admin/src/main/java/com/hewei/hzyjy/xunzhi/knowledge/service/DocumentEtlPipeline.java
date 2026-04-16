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

    private static final int MAX_CHUNK_SIZE = 800;
    private static final int OVERLAP_SIZE = 100;

    private final ElasticsearchVectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 异步处理文档。参数为 byte[] 而非 MultipartFile，避免主请求线程关闭后临时文件被清理导致读取失败。
     */
    @Async
    public void process(Long kbId, String username, byte[] fileBytes, String fileName) {
        String docId = UUID.randomUUID().toString();
        String text;
        try {
            String fileType = getFileExtension(fileName);
            text = extractText(fileBytes, fileType);
        } catch (Exception e) {
            log.error("Failed to parse file: {}", fileName, e);
            return;
        }

        if (StrUtil.isBlank(text)) {
            log.warn("Extracted text is empty for file: {}", fileName);
            return;
        }

        String fileType = getFileExtension(fileName);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKbId(kbId);
        doc.setDocId(docId);
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize((long) fileBytes.length);
        doc.setUsername(username);
        doc.setStatus(1);
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());
        documentRepository.save(doc);

        List<String> chunks = smartChunk(text);
        List<Map<String, Object>> chunkDocs = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = docId + "_" + i;
            List<Float> embedding = embeddingService.embed(chunk);

            Map<String, Object> chunkDoc = new HashMap<>();
            chunkDoc.put("chunk_id", chunkId);
            chunkDoc.put("doc_id", docId);
            chunkDoc.put("kb_id", kbId);
            chunkDoc.put("content", chunk);
            chunkDoc.put("file_name", fileName);
            chunkDoc.put("chunk_index", i);
            chunkDoc.put("embedding", embedding.stream().map(Float::doubleValue).toList());
            chunkDoc.put("metadata", Map.of("char_count", chunk.length()));
            chunkDocs.add(chunkDoc);
        }

        vectorStore.indexChunks(kbId, chunkDocs);

        doc.setChunkCount(chunks.size());
        doc.setStatus(2);
        doc.setUpdateTime(LocalDateTime.now());
        documentRepository.save(doc);

        // 更新知识库的文档数和分块数
        updateKnowledgeBaseCounts(kbId);

        log.info("ETL pipeline completed, kbId={}, docId={}, chunks={}", kbId, docId, chunks.size());
    }

    public void deleteDocument(Long kbId, String docId) {
        vectorStore.deleteByDocId(kbId, docId);
        documentRepository.deleteByDocId(docId);
        updateKnowledgeBaseCounts(kbId);
    }

    private void updateKnowledgeBaseCounts(Long kbId) {
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

    private List<String> smartChunk(String text) {
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

            if (currentChunk.length() + trimmed.length() > MAX_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                if (currentChunk.length() > OVERLAP_SIZE) {
                    String overlap = currentChunk.substring(Math.max(0, currentChunk.length() - OVERLAP_SIZE));
                    currentChunk = new StringBuilder(overlap);
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);

            while (currentChunk.length() > MAX_CHUNK_SIZE * 2) {
                int splitPos = MAX_CHUNK_SIZE;
                int sentenceEnd = findSentenceEnd(currentChunk.toString(), MAX_CHUNK_SIZE);
                if (sentenceEnd > 0 && sentenceEnd < MAX_CHUNK_SIZE + 200) {
                    splitPos = sentenceEnd;
                }
                chunks.add(currentChunk.substring(0, splitPos).trim());
                String remaining = currentChunk.substring(Math.max(0, splitPos - OVERLAP_SIZE));
                currentChunk = new StringBuilder(remaining);
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private int findSentenceEnd(String text, int startPos) {
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
