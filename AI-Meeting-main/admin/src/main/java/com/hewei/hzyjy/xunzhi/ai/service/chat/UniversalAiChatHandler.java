package com.hewei.hzyjy.xunzhi.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiChatStreamRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.enums.AiPropritiesType;
import com.hewei.hzyjy.xunzhi.ai.service.ApiKeyCryptoService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 通用 AI 聊天处理器，基于 Spring AI 实现
 * 支持 OpenAI、Doubao、Spark 等兼容 OpenAI 接口的模型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniversalAiChatHandler implements AiChatHandler {

    private final ApiKeyCryptoService apiKeyCryptoService;

    @Override
    public String getType() {
        return "universal";
    }

    public boolean supports(String type) {
        return AiPropritiesType.isSupported(type);
    }

    @Override
    public void streamToSink(AiPropertiesDO aiProperties, String userMessage, List<AiMessageHistoryRespDTO> historyMessages,
                             FluxSink<String> sink, AIContentAccumulator accumulator) throws Exception {
        ChatClient chatClient = createChatClient(aiProperties);
        List<Message> messages = buildMessages(aiProperties, userMessage, historyMessages);

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] streamError = new Throwable[1];

        chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .subscribe(
                        chatResponse -> {
                            try {
                                Generation generation = chatResponse.getResult();
                                if (generation != null) {
                                    String content = generation.getOutput().getText();
                                    if (StrUtil.isNotEmpty(content)) {
                                        AiChatStreamRespDTO resp = AiChatStreamRespDTO.builder()
                                                .type("content")
                                                .content(content)
                                                .build();
                                        sink.next(JSON.toJSONString(resp));
                                        accumulator.appendSimpleContent(content);
                                    }

                                    String reasoning = null;
                                    try {
                                        java.lang.reflect.Method getReasoningContent = generation.getOutput().getClass().getMethod("getReasoningContent");
                                        Object reasoningVal = getReasoningContent.invoke(generation.getOutput());
                                        if (reasoningVal != null) {
                                            reasoning = reasoningVal.toString();
                                        }
                                    } catch (Exception ignore) {
                                    }

                                    if (reasoning == null) {
                                        Object reasoningObj = generation.getOutput().getMetadata().get("reasoningContent");
                                        if (reasoningObj != null) {
                                            reasoning = reasoningObj.toString();
                                        }
                                    }

                                    if (StrUtil.isNotEmpty(reasoning)) {
                                        AiChatStreamRespDTO resp = AiChatStreamRespDTO.builder()
                                                .type("reasoning_content")
                                                .content(reasoning)
                                                .build();
                                        sink.next(JSON.toJSONString(resp));
                                        accumulator.appendReasoningChunk(reasoning.getBytes());
                                    }
                                }
                            } catch (Exception e) {
                                log.error("流式响应处理错误", e);
                                streamError[0] = e;
                                sink.error(e);
                                latch.countDown();
                            }
                        },
                        error -> {
                            log.error("流式响应发生错误", error);
                            streamError[0] = error;
                            sink.error(error);
                            latch.countDown();
                        },
                        latch::countDown
                );

        if (!latch.await(5, TimeUnit.MINUTES)) {
            throw new RuntimeException("AI 响应超时");
        }

        if (streamError[0] != null) {
            throw new RuntimeException(streamError[0]);
        }
    }

    /**
     * 非流式一次性调用（内部场景复用用户模型，如复习弱项抽取）
     */
    public String completeOnce(AiPropertiesDO aiProperties, String systemPrompt, String userPrompt) {
        ChatClient chatClient = createChatClient(aiProperties);
        List<Message> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(userPrompt));
        return chatClient.prompt().messages(messages).call().content();
    }

    private ChatClient createChatClient(AiPropertiesDO aiProperties) {
        String baseUrl = aiProperties.getApiUrl();
        // 用户私有配置的 Key 为 enc:v1: 前缀密文，使用前解密；存量明文原样返回
        String apiKey = apiKeyCryptoService.decryptIfNeeded(aiProperties.getApiKey());

        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = AiPropritiesType.getByType(aiProperties.getAiType()).getDefaultBaseUrl();
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new ClientException("AI API Key 未配置");
        }

        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultHeaders(headers -> {
                    if (StrUtil.isNotBlank(aiProperties.getProjectId())) {
                        headers.add("OpenAI-Project", aiProperties.getProjectId());
                    }
                    if (StrUtil.isNotBlank(aiProperties.getOrganizationId())) {
                        headers.add("OpenAI-Organization", aiProperties.getOrganizationId());
                    }
                });

        if (AiPropritiesType.DEEPSEEK.getType().equalsIgnoreCase(aiProperties.getAiType())) {
            baseUrl = baseUrl.replaceAll("/+$", "");
        }

        OpenAiApi openAiApi = new OpenAiApi(
                baseUrl,
                new SimpleApiKey(apiKey),
                new LinkedMultiValueMap<>(),
                "/chat/completions",
                "/embeddings",
                restClientBuilder,
                WebClient.builder(),
                new DefaultResponseErrorHandler()
        );

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(aiProperties.getModelName());

        if (aiProperties.getMaxTokens() != null) {
            optionsBuilder.maxTokens(aiProperties.getMaxTokens());
        }

        OpenAiChatOptions options = optionsBuilder.build();
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                options,
                toolCallingManager,
                RetryTemplate.defaultInstance(),
                ObservationRegistry.NOOP
        );

        return ChatClient.builder(chatModel)
                .defaultOptions(options)
                .build();
    }

    private List<Message> buildMessages(AiPropertiesDO aiProperties, String userMessage, List<AiMessageHistoryRespDTO> historyMessages) {
        List<Message> messages = new ArrayList<>();

        if (StrUtil.isNotBlank(aiProperties.getSystemPrompt())) {
            messages.add(new SystemMessage(aiProperties.getSystemPrompt()));
        }

        if (CollUtil.isNotEmpty(historyMessages)) {
            for (AiMessageHistoryRespDTO history : historyMessages) {
                String content = history.getMessageContent();
                if (history.getMessageType() != null && history.getMessageType() == 1) {
                    messages.add(new UserMessage(content));
                } else {
                    messages.add(new AssistantMessage(content));
                }
            }
        }

        messages.add(new UserMessage(userMessage));
        return messages;
    }
}
