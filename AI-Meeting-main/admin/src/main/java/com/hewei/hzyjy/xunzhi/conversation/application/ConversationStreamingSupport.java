package com.hewei.hzyjy.xunzhi.conversation.application;

import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@Slf4j
public class ConversationStreamingSupport {

    public <H> void execute(ConversationStreamRequest<H> request) {
        long startTime = System.currentTimeMillis();
        try {
            List<H> historyMessages = request.historySupplier.get();
            request.userMessageSaver.run();
            request.streamExecutor.execute(historyMessages, request.accumulator);

            int responseTime = (int) (System.currentTimeMillis() - startTime);
            int assistantMessageSeq = request.assistantMessageSaver.apply(new AssistantMessagePayload(
                    request.accumulator.getFullContent(),
                    request.accumulator.getFullReasoningContent(),
                    responseTime,
                    null
            ));
            if (request.conversationUpdater != null) {
                request.conversationUpdater.accept(assistantMessageSeq);
            }
            if (request.successHandler != null) {
                request.successHandler.run();
            }
        } catch (Exception ex) {
            log.error("Conversation streaming failed, sessionId={}", request.sessionId, ex);
            int responseTime = (int) (System.currentTimeMillis() - startTime);
            try {
                request.assistantMessageSaver.apply(new AssistantMessagePayload(
                        request.defaultErrorContent,
                        null,
                        responseTime,
                        ex.getMessage()
                ));
            } catch (Exception persistenceEx) {
                log.error("Failed to persist conversation error message, sessionId={}",
                        request.sessionId, persistenceEx);
            }
            if (request.errorHandler != null) {
                request.errorHandler.accept(ex);
            }
        }
    }

    @Getter
    @Builder
    public static class ConversationStreamRequest<H> {
        private final String sessionId;
        private final String defaultErrorContent;
        private final AIContentAccumulator accumulator;
        private final ConversationHistorySupplier<H> historySupplier;
        private final CheckedRunnable userMessageSaver;
        private final ConversationStreamExecutor<H> streamExecutor;
        private final Function<AssistantMessagePayload, Integer> assistantMessageSaver;
        private final Consumer<Integer> conversationUpdater;
        private final Runnable successHandler;
        private final Consumer<Exception> errorHandler;
    }

    public record AssistantMessagePayload(
            String content,
            String reasoningContent,
            int responseTime,
            String errorMessage
    ) {
    }

    @FunctionalInterface
    public interface ConversationHistorySupplier<H> {
        List<H> get() throws Exception;
    }

    @FunctionalInterface
    public interface ConversationStreamExecutor<H> {
        void execute(List<H> historyMessages, AIContentAccumulator accumulator) throws Exception;
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }
}
