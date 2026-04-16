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
}
