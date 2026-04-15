package com.hewei.hzyjy.xunzhi.knowledge.api;

import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.knowledge.api.io.req.KnowledgeBaseCreateReqDTO;
import com.hewei.hzyjy.xunzhi.knowledge.api.io.resp.KnowledgeBaseRespDTO;
import com.hewei.hzyjy.xunzhi.knowledge.api.io.resp.KnowledgeDocumentRespDTO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import com.hewei.hzyjy.xunzhi.knowledge.service.DocumentEtlPipeline;
import com.hewei.hzyjy.xunzhi.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/xunzhi/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentEtlPipeline documentEtlPipeline;

    @PostMapping
    public Result<KnowledgeBaseRespDTO> create(@RequestBody KnowledgeBaseCreateReqDTO requestParam,
                                                @CurrentUser String username) {
        KnowledgeBaseDO kb = knowledgeBaseService.createKnowledgeBase(
                requestParam.getName(), requestParam.getDescription(), username);
        return Results.success(toRespDTO(kb));
    }

    @DeleteMapping("/{kbId}")
    public Result<Void> delete(@PathVariable Long kbId, @CurrentUser String username) {
        knowledgeBaseService.deleteKnowledgeBase(kbId, username);
        return Results.success();
    }

    @GetMapping
    public Result<List<KnowledgeBaseRespDTO>> list(@CurrentUser String username) {
        List<KnowledgeBaseDO> kbs = knowledgeBaseService.listUserKnowledgeBases(username);
        return Results.success(kbs.stream().map(this::toRespDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{kbId}")
    public Result<KnowledgeBaseRespDTO> get(@PathVariable Long kbId, @CurrentUser String username) {
        KnowledgeBaseDO kb = knowledgeBaseService.getKnowledgeBase(kbId, username);
        return Results.success(toRespDTO(kb));
    }

    @PostMapping("/{kbId}/documents")
    public Result<Void> uploadDocument(@PathVariable Long kbId,
                                        @RequestParam("file") MultipartFile file,
                                        @CurrentUser String username) {
        if (file.isEmpty()) {
            return Results.failure(new ClientException("上传文件不能为空"));
        }
        String fileName = file.getOriginalFilename();
        if (!DocumentEtlPipeline.isSupportedFileType(fileName)) {
            return Results.failure(new ClientException("不支持的文件类型，仅支持 txt, md, pdf, doc, docx"));
        }
        // 在主请求线程中同步读取字节，避免异步线程中 MultipartFile 临时文件被清理
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read multipart file", e);
            return Results.failure(new ClientException("文件读取失败"));
        }
        documentEtlPipeline.process(kbId, username, fileBytes, fileName);
        return Results.success();
    }

    @GetMapping("/{kbId}/documents")
    public Result<List<KnowledgeDocumentRespDTO>> listDocuments(@PathVariable Long kbId,
                                                                  @CurrentUser String username) {
        List<KnowledgeDocument> docs = knowledgeBaseService.listDocuments(kbId, username);
        return Results.success(docs.stream().map(this::toDocRespDTO).collect(Collectors.toList()));
    }

    @DeleteMapping("/{kbId}/documents/{docId}")
    public Result<Void> deleteDocument(@PathVariable Long kbId,
                                        @PathVariable String docId,
                                        @CurrentUser String username) {
        documentEtlPipeline.deleteDocument(kbId, docId);
        return Results.success();
    }

    private KnowledgeBaseRespDTO toRespDTO(KnowledgeBaseDO kb) {
        KnowledgeBaseRespDTO dto = new KnowledgeBaseRespDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setDescription(kb.getDescription());
        dto.setDocumentCount(kb.getDocumentCount());
        dto.setChunkCount(kb.getChunkCount());
        dto.setCreateTime(kb.getCreateTime());
        return dto;
    }

    private KnowledgeDocumentRespDTO toDocRespDTO(KnowledgeDocument doc) {
        KnowledgeDocumentRespDTO dto = new KnowledgeDocumentRespDTO();
        dto.setId(doc.getId());
        dto.setDocId(doc.getDocId());
        dto.setFileName(doc.getFileName());
        dto.setFileType(doc.getFileType());
        dto.setFileSize(doc.getFileSize());
        dto.setStatus(doc.getStatus());
        dto.setChunkCount(doc.getChunkCount());
        dto.setCreateTime(doc.getCreateTime());
        return dto;
    }
}
