package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import cn.xfyun.api.SparkIatClient;
import cn.xfyun.model.sparkiat.response.SparkIatResponse;
import cn.xfyun.service.sparkiat.AbstractSparkIatWebSocketListener;
import cn.xfyun.util.StringUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 讯飞星火语音转写核心服务
 * 负责处理语音转写的核心业务逻辑
 */
@Slf4j
@Service
public class SparkIatService {

    /**
     * 执行语音转写
     * @param audioFile 音频文件
     * @param config 转写配置
     * @param partialResultCallback 中间结果回调
     * @return 转写结果
     * @throws Exception 转写异常
     */
    public String  executeTranscription(File audioFile, SparkIatUtil.IatConfig config,
                                      Consumer<String> partialResultCallback) throws Exception {
        validateInput(audioFile, config);
        
        SparkIatClient client = createClient(config);
        TranscriptionContext context = new TranscriptionContext();
        CountDownLatch latch = new CountDownLatch(1);
        
        client.send(audioFile, createWebSocketListener(context, latch, partialResultCallback));
        
        return waitForResult(client, context, latch, config.getTimeoutSeconds());
    }
    
    /**
     * 验证输入参数
     */
    private void validateInput(File audioFile, SparkIatUtil.IatConfig config) {
        if (audioFile == null || !audioFile.exists()) {
            throw new IllegalArgumentException("音频文件不存在: " + 
                (audioFile != null ? audioFile.getPath() : "null"));
        }
        if (config == null) {
            throw new IllegalArgumentException("转写配置不能为空");
        }
        if (config.getAppId() == null || config.getApiKey() == null || config.getApiSecret() == null) {
            throw new IllegalArgumentException("API配置信息不完整");
        }
    }
    
    /**
     * 创建客户端
     */
    private SparkIatClient createClient(SparkIatUtil.IatConfig config) {
        return new SparkIatClient.Builder()
                .signature(config.getAppId(), config.getApiKey(), config.getApiSecret(), 
                          config.getModel().getCode())
                .dwa(config.getDwa())
                .build();
    }
    
    /**
     * 创建WebSocket监听器
     */
    private AbstractSparkIatWebSocketListener createWebSocketListener(
            TranscriptionContext context, CountDownLatch latch, Consumer<String> partialResultCallback) {
        return new AbstractSparkIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, SparkIatResponse resp) {
                try {
                    handleResponse(resp, context, partialResultCallback);
                    if (isTranscriptionComplete(resp)) {
                        context.setFinalResult(buildFinalResult(context.getContentMap()));
                        latch.countDown();
                    }
                } catch (Exception e) {
                    log.error("处理转写结果时发生异常", e);
                    context.setException(e);
                    latch.countDown();
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                log.error("语音转写连接失败", t);
                context.setException(new RuntimeException("语音转写连接失败: " + t.getMessage(), t));
                latch.countDown();
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                log.info("语音转写连接关闭，code: {}, reason: {}", code, reason);
                latch.countDown();
            }
        };
    }
    
    /**
     * 处理响应
     */
    private void handleResponse(SparkIatResponse resp, TranscriptionContext context, 
                               Consumer<String> partialResultCallback) {
        if (resp.getHeader().getCode() != 0) {
            String errorMsg = String.format("转写失败 - code: %d, error: %s, sid: %s", 
                    resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        context.setSessionId(resp.getHeader().getSid());
        
        if (resp.getPayload() != null && resp.getPayload().getResult() != null) {
            processTextResult(resp.getPayload().getResult().getText(), context, partialResultCallback);
        }
    }
    
    /**
     * 处理文本结果
     */
    private void processTextResult(String textData, TranscriptionContext context, 
                                  Consumer<String> partialResultCallback) {
        if (textData == null) return;
        
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(textData);
            String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
            SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, 
                    SparkIatResponse.JsonParseText.class);
            
            StringBuilder reqResult = extractWsContent(jsonParseText);
            updateContentMap(jsonParseText, reqResult.toString(), context.getContentMap());
            
            if (partialResultCallback != null) {
                String currentResult = buildFinalResult(context.getContentMap());
                partialResultCallback.accept(currentResult);
            }
        } catch (Exception e) {
            log.warn("解析文本结果失败: {}", e.getMessage());
        }
    }
    
    /**
     * 更新内容映射
     */
    private void updateContentMap(SparkIatResponse.JsonParseText jsonParseText, String content, 
                                 Map<Integer, String> contentMap) {
        if ("apd".equals(jsonParseText.getPgs())) {
            contentMap.put(jsonParseText.getSn(), content);
            log.debug("拼接结果: {}", content);
        } else if ("rpl".equals(jsonParseText.getPgs())) {
            List<Integer> rg = jsonParseText.getRg();
            int startIndex = rg.get(0);
            int endIndex = rg.get(1);
            for (int i = startIndex; i <= endIndex; i++) {
                contentMap.remove(i);
            }
            contentMap.put(jsonParseText.getSn(), content);
            log.debug("替换结果: {}", content);
        }
    }
    
    /**
     * 提取WebSocket内容
     */
    private StringBuilder extractWsContent(SparkIatResponse.JsonParseText jsonParseText) {
        StringBuilder result = new StringBuilder();
        List<SparkIatResponse.Ws> wsList = jsonParseText.getWs();
        for (SparkIatResponse.Ws ws : wsList) {
            List<SparkIatResponse.Cw> cwList = ws.getCw();
            for (SparkIatResponse.Cw cw : cwList) {
                result.append(cw.getW());
            }
        }
        return result;
    }
    
    /**
     * 构建最终结果
     */
    private String buildFinalResult(Map<Integer, String> contentMap) {
        StringBuilder result = new StringBuilder();
        for (String part : contentMap.values()) {
            result.append(part);
        }
        return result.toString();
    }
    
    /**
     * 检查转写是否完成
     */
    private boolean isTranscriptionComplete(SparkIatResponse resp) {
        return resp.getPayload() != null && 
               resp.getPayload().getResult() != null && 
               resp.getPayload().getResult().getStatus() == 2;
    }
    
    /**
     * 等待转写结果
     */
    private String waitForResult(SparkIatClient client, TranscriptionContext context, 
                                CountDownLatch latch, int timeoutSeconds) throws Exception {
        boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!completed) {
            client.closeWebsocket();
            throw new RuntimeException("语音转写超时，超过 " + timeoutSeconds + " 秒");
        }
        
        if (context.getException() != null) {
            throw context.getException();
        }
        
        String result = context.getFinalResult();
        log.info("语音转写完成，session: {}, 结果: {}", context.getSessionId(), result);
        return result;
    }
    
    /**
     * 转写上下文
     */
    private static class TranscriptionContext {
        private final Map<Integer, String> contentMap = new TreeMap<>();
        @Setter
        private String sessionId;
        @Setter
        private String finalResult;
        @Setter
        private Exception exception;
        
        public Map<Integer, String> getContentMap() { return contentMap; }
        public String getSessionId() { return sessionId; }

        public String getFinalResult() { return finalResult; }

        public Exception getException() { return exception; }
    }
}