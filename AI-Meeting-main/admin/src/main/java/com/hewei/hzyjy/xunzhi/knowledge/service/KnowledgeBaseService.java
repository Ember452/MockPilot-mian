package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeBaseDO createKnowledgeBase(String name, String description, String username);

    void deleteKnowledgeBase(Long kbId, String username);

    List<KnowledgeBaseDO> listUserKnowledgeBases(String username);

    KnowledgeBaseDO getKnowledgeBase(Long kbId, String username);

    List<KnowledgeDocument> listDocuments(Long kbId, String username);

    /**
     * 上传前校验知识库与当前 embedding 模型兼容，不兼容抛 ClientException；
     * legacy 库（未绑定模型）校验通过后回填当前模型标识。
     */
    void ensureEmbeddingCompatible(Long kbId, String username);
}
