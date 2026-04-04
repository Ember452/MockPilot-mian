package com.hewei.hzyjy.xunzhi.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiConversationRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiConversation;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.dao.repository.AiConversationRepository;
import com.hewei.hzyjy.xunzhi.ai.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {

    private final AiConversationRepository aiConversationRepository;
    private final AiPropertiesService aiPropertiesService;

    @Override
    public String createConversation(String username, Long aiId, String firstMessage) {
        AiPropertiesDO aiProperties = aiPropertiesService.getById(aiId);
        if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
            throw new ClientException("AI config does not exist or is disabled");
        }

        String sessionId = IdUtil.getSnowflakeNextIdStr();
        String title = generateTitle(firstMessage);

        AiConversation conversation = new AiConversation();
        conversation.setSessionId(sessionId);
        conversation.setUsername(username);
        conversation.setAiId(aiId);
        conversation.setTitle(title);
        conversation.setStatus(1);
        conversation.setMessageCount(0);
        conversation.setLastMessageTime(new Date());
        conversation.setCreateTime(new Date());
        conversation.setUpdateTime(new Date());
        conversation.setDelFlag(0);

        aiConversationRepository.save(conversation);
        return sessionId;
    }

    @Override
    public AiSessionCreateRespDTO createConversationWithTitle(String username, Long aiId, String firstMessage) {
        String sessionId = createConversation(username, aiId, firstMessage);
        AiSessionCreateRespDTO respDTO = new AiSessionCreateRespDTO();
        respDTO.setSessionId(sessionId);
        respDTO.setConversationTitle(generateTitle(firstMessage));
        return respDTO;
    }

    @Override
    public IPage<AiConversationRespDTO> pageConversations(String username, AiConversationPageReqDTO requestParam) {
        Pageable pageable = PageRequest.of(requestParam.getCurrent() - 1, requestParam.getSize());
        org.springframework.data.domain.Page<AiConversation> conversationPage;

        if (requestParam.getAiId() != null) {
            conversationPage = aiConversationRepository
                    .findByUsernameAndAiIdAndDelFlagOrderByCreateTimeDesc(username, requestParam.getAiId(), 0, pageable);
        } else {
            conversationPage = aiConversationRepository
                    .findByUsernameAndDelFlagOrderByCreateTimeDesc(username, 0, pageable);
        }

        Page<AiConversationRespDTO> resultPage = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        resultPage.setTotal(conversationPage.getTotalElements());

        List<AiConversationRespDTO> records = conversationPage.getContent().stream()
                .map(conversation -> {
                    AiConversationRespDTO respDTO = new AiConversationRespDTO();
                    BeanUtil.copyProperties(conversation, respDTO);
                    AiPropertiesDO aiProperties = aiPropertiesService.getById(conversation.getAiId());
                    if (aiProperties != null) {
                        respDTO.setAiName(aiProperties.getAiName());
                    }
                    return respDTO;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public void updateConversation(String sessionId, Integer messageSeq, String title) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (conversationOpt.isEmpty()) {
            return;
        }
        AiConversation conversation = conversationOpt.get();
        if (messageSeq != null) {
            conversation.setMessageCount(messageSeq);
        }
        if (StrUtil.isNotBlank(title)) {
            conversation.setTitle(title);
        }
        conversation.setLastMessageTime(new Date());
        conversation.setUpdateTime(new Date());
        aiConversationRepository.save(conversation);
    }

    @Override
    public void updateConversation(String sessionId, Integer messageSeq, String title, String username) {
        requireOwnedConversation(sessionId, username);
        updateConversation(sessionId, messageSeq, title);
    }

    @Override
    public void endConversation(String sessionId) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (conversationOpt.isEmpty()) {
            throw new ClientException("Conversation does not exist");
        }
        AiConversation conversation = conversationOpt.get();
        conversation.setStatus(2);
        conversation.setUpdateTime(new Date());
        aiConversationRepository.save(conversation);
    }

    @Override
    public void endConversation(String sessionId, String username) {
        requireOwnedConversation(sessionId, username);
        endConversation(sessionId);
    }

    @Override
    public void deleteConversation(String sessionId) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (conversationOpt.isEmpty()) {
            throw new ClientException("Conversation does not exist");
        }
        AiConversation conversation = conversationOpt.get();
        conversation.setDelFlag(1);
        conversation.setUpdateTime(new Date());
        aiConversationRepository.save(conversation);
    }

    @Override
    public void deleteConversation(String sessionId, String username) {
        requireOwnedConversation(sessionId, username);
        deleteConversation(sessionId);
    }

    @Override
    public void clearAllConversations(String username) {
        if (StrUtil.isBlank(username)) {
            throw new ClientException("username cannot be empty");
        }
        List<AiConversation> conversations = aiConversationRepository
                .findByUsernameAndDelFlagOrderByCreateTimeDesc(username, 0);
        Date now = new Date();
        for (AiConversation conversation : conversations) {
            conversation.setDelFlag(1);
            conversation.setUpdateTime(now);
        }
        aiConversationRepository.saveAll(conversations);
    }

    @Override
    public AiConversationRespDTO getConversationBySessionId(String sessionId) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (conversationOpt.isEmpty()) {
            throw new ClientException("Conversation does not exist");
        }
        AiConversation conversation = conversationOpt.get();
        AiConversationRespDTO respDTO = new AiConversationRespDTO();
        BeanUtil.copyProperties(conversation, respDTO);
        AiPropertiesDO aiProperties = aiPropertiesService.getById(conversation.getAiId());
        if (aiProperties != null) {
            respDTO.setAiName(aiProperties.getAiName());
        }
        return respDTO;
    }

    @Override
    public AiConversationRespDTO getConversationBySessionId(String sessionId, String username) {
        requireOwnedConversation(sessionId, username);
        return getConversationBySessionId(sessionId);
    }

    @Override
    public void requireOwnedConversation(String sessionId, String username) {
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException("sessionId cannot be empty");
        }
        if (StrUtil.isBlank(username)) {
            throw new ClientException("username cannot be empty");
        }
        AiConversation conversation = aiConversationRepository
                .findBySessionIdAndDelFlag(sessionId, 0)
                .orElseThrow(() -> new ClientException("Conversation does not exist"));
        if (!username.equals(conversation.getUsername())) {
            throw new ClientException("No permission to access this conversation");
        }
    }

    @Override
    public List<String> listOwnedSessionIds(String username) {
        if (StrUtil.isBlank(username)) {
            throw new ClientException("username cannot be empty");
        }
        return aiConversationRepository.findByUsernameAndDelFlagOrderByCreateTimeDesc(username, 0)
                .stream()
                .map(AiConversation::getSessionId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private String generateTitle(String firstMessage) {
        if (StrUtil.isBlank(firstMessage)) {
            return "New Conversation";
        }
        if (firstMessage.length() <= 20) {
            return firstMessage;
        }
        return firstMessage.substring(0, 20) + "...";
    }
}
