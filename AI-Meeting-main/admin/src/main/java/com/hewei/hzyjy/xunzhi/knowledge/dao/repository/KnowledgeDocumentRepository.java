package com.hewei.hzyjy.xunzhi.knowledge.dao.repository;

import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findByKbIdAndUsername(Long kbId, String username);

    List<KnowledgeDocument> findByDocId(String docId);

    void deleteByDocId(String docId);
}
