package com.hewei.hzyjy.xunzhi.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.hewei.hzyjy.xunzhi.knowledge.dao.repository.KnowledgeDocumentRepository;
import com.hewei.hzyjy.xunzhi.knowledge.service.ElasticsearchVectorStore;
import com.hewei.hzyjy.xunzhi.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentRepository documentRepository;
    private final ElasticsearchVectorStore vectorStore;

    @Override
    @Transactional
    public KnowledgeBaseDO createKnowledgeBase(String name, String description, String username) {
        if (StrUtil.isBlank(name)) {
            throw new ClientException("知识库名称不能为空");
        }

        KnowledgeBaseDO kb = new KnowledgeBaseDO();
        kb.setName(name.trim());
        kb.setDescription(StrUtil.blankToDefault(description, ""));
        kb.setUsername(username);
        kb.setDocumentCount(0);
        kb.setChunkCount(0);
        kb.setIsEnabled(1);
        kb.setCreateTime(LocalDateTime.now());
        kb.setUpdateTime(LocalDateTime.now());
        kb.setDelFlag(0);
        knowledgeBaseMapper.insert(kb);

        vectorStore.createIndexIfNotExists(kb.getId());

        return kb;
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long kbId, String username) {
        KnowledgeBaseDO kb = requireOwnKnowledgeBase(kbId, username);

        kb.setDelFlag(1);
        kb.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);

        vectorStore.deleteIndex(kbId);
    }

    @Override
    public List<KnowledgeBaseDO> listUserKnowledgeBases(String username) {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .eq(KnowledgeBaseDO::getUsername, username)
                        .eq(KnowledgeBaseDO::getDelFlag, 0)
                        .eq(KnowledgeBaseDO::getIsEnabled, 1)
                        .orderByDesc(KnowledgeBaseDO::getCreateTime)
        );
    }

    @Override
    public KnowledgeBaseDO getKnowledgeBase(Long kbId, String username) {
        return requireOwnKnowledgeBase(kbId, username);
    }

    @Override
    public List<KnowledgeDocument> listDocuments(Long kbId, String username) {
        requireOwnKnowledgeBase(kbId, username);
        return documentRepository.findByKbIdAndUsername(kbId, username);
    }

    private KnowledgeBaseDO requireOwnKnowledgeBase(Long kbId, String username) {
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDelFlag() == 1) {
            throw new ClientException("知识库不存在");
        }
        if (!kb.getUsername().equals(username)) {
            throw new ClientException("无权访问该知识库");
        }
        return kb;
    }
}
