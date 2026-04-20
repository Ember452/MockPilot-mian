package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AIContentAccumulator {
    private final StringBuilder contentBuilder = new StringBuilder();
    private final StringBuilder reasoningBuilder = new StringBuilder();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析 JSON 并累积内容
     */
    public void appendChunk(byte[] chunk) {
        try {
            String chunkStr = new String(chunk);
            
            // 处理SSE格式：移除"data: "前缀
            String jsonStr = chunkStr;
            if (chunkStr.startsWith("data: ")) {
                jsonStr = chunkStr.substring(6); // 移除"data: "前缀
            }
            
            // 跳过空行或非JSON数据
            jsonStr = jsonStr.trim();
            if (jsonStr.isEmpty() || !jsonStr.startsWith("{")) {
                return;
            }
            
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode choices = root.path("choices");
            if (choices.isArray()) {
                for (JsonNode choice : choices) {
                    JsonNode delta = choice.path("delta");
                    JsonNode content = delta.path("content");
                    if (content.isTextual()) {
                        contentBuilder.append(content.asText());
                    }
                    // 处理 DeepSeek 深度思考内容
                    JsonNode reasoning = delta.path("reasoning_content");
                    if (reasoning.isTextual()) {
                        reasoningBuilder.append(reasoning.asText());
                    }
                }
            }
        } catch (IOException e) {
            // 记录解析错误（用于调试）
            System.err.println("JSON解析错误: " + e.getMessage() + ", 数据: " + new String(chunk));
        }
    }

    /**
     * 专门累积 reasoning_content (用于 Spring AI 返回的 chunk 对象)
     */
    public void appendReasoningChunk(byte[] chunk) {
        if (chunk != null) {
            reasoningBuilder.append(new String(chunk));
        }
    }

    /**
     * 获取完整内容
     */
    public String getFullContent() {
        return contentBuilder.toString();
    }

    /**
     * 获取完整思考过程
     */
    public String getFullReasoningContent() {
        return reasoningBuilder.toString();
    }

    /**
     * 重置累积器
     */
    public void reset() {
        contentBuilder.setLength(0);
        reasoningBuilder.setLength(0);
    }

    /**
     * 处理Coze工作流消息并累积内容
     * 解析格式：WorkflowEventMessage(content=嘿, nodeTitle=结束, nodeSeqID=0, nodeIsFinish=false, token=null, ext=null, usage=null)
     */
    public void appendCozeWorkflowMessage(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return;
            }

            // 解析JSON格式的Coze工作流消息
            if (message.trim().startsWith("{") && message.trim().endsWith("}")) {
                try {
                    // 先尝试修复JSON中的换行符问题
                    String fixedMessage = fixJsonString(message);
                    JsonNode root = objectMapper.readTree(fixedMessage);
                    
                    // 检查是否是content类型的消息
                    JsonNode typeNode = root.path("type");
                    if (typeNode.isTextual() && "content".equals(typeNode.asText())) {
                        JsonNode contentNode = root.path("content");
                        if (contentNode.isTextual()) {
                            String content = contentNode.asText();
                            
                            // 解析WorkflowEventMessage格式
                            if (content.startsWith("WorkflowEventMessage(") && content.endsWith(")")) {
                                String messageContent = extractContentFromWorkflowMessage(content);
                                if (messageContent != null && !messageContent.trim().isEmpty()) {
                                    contentBuilder.append(messageContent);
                                }
                            } else {
                                // 直接添加content内容
                                contentBuilder.append(content);
                            }
                        }
                    }
                } catch (Exception jsonException) {
                    // JSON解析失败，尝试直接提取WorkflowEventMessage内容
                    handleJsonParseFailure(message);
                }
            } else {
                // 直接处理WorkflowEventMessage格式的字符串
                if (message.startsWith("WorkflowEventMessage(") && message.endsWith(")")) {
                    String messageContent = extractContentFromWorkflowMessage(message);
                    if (messageContent != null && !messageContent.trim().isEmpty()) {
                        contentBuilder.append(messageContent);
                    }
                } else {
                    // 其他格式直接添加
                    contentBuilder.append(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Coze工作流消息解析错误: " + e.getMessage() + ", 数据: " + message);
        }
    }

    /**
     * 修复JSON字符串中的换行符和其他特殊字符
     */
    private String fixJsonString(String jsonString) {
        if (jsonString == null) {
            return null;
        }
        
        // 处理JSON字符串中的换行符和其他控制字符
        return jsonString
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    /**
     * 处理JSON解析失败的情况
     */
    private void handleJsonParseFailure(String message) {
        try {
            if (message.contains("WorkflowEventMessage(")) {
                int startIndex = message.indexOf("WorkflowEventMessage(");
                int endIndex = message.lastIndexOf(")");
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    String workflowMessage = message.substring(startIndex, endIndex + 1);
                    String messageContent = extractContentFromWorkflowMessage(workflowMessage);
                    if (messageContent != null && !messageContent.trim().isEmpty()) {
                        contentBuilder.append(messageContent);
                    }
                }
            }
        } catch (Exception fallbackException) {
            System.err.println("备用解析也失败: " + fallbackException.getMessage());
        }
    }

    /**
     * 从WorkflowEventMessage中提取content内容
     * 格式：WorkflowEventMessage(content=嘿, nodeTitle=结束, nodeSeqID=0, nodeIsFinish=false, token=null, ext=null, usage=null)
     */
    private String extractContentFromWorkflowMessage(String workflowMessage) {
        try {
            // 移除前缀和后缀
            String params = workflowMessage.substring("WorkflowEventMessage(".length(), workflowMessage.length() - 1);
            
            // 由于content可能包含逗号和换行符，需要更智能的解析
            int contentStart = params.indexOf("content=");
            if (contentStart == -1) {
                return null;
            }
            
            contentStart += "content=".length();
            
            // 查找content参数的结束位置
            int contentEnd = findContentEnd(params, contentStart);
            
            if (contentEnd == -1) {
                // 如果找不到结束位置，取到下一个参数开始
                String remainingParams = params.substring(contentStart);
                int nextParamIndex = remainingParams.indexOf(", nodeTitle=");
                if (nextParamIndex != -1) {
                    contentEnd = contentStart + nextParamIndex;
                } else {
                    contentEnd = params.length();
                }
            }
            
            String content = params.substring(contentStart, contentEnd).trim();
            
            // 移除可能的引号
            if (content.startsWith("\"") && content.endsWith("\"")) {
                content = content.substring(1, content.length() - 1);
            }
            
            return content;
        } catch (Exception e) {
            System.err.println("提取WorkflowEventMessage内容失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 查找content参数的结束位置
     */
    private int findContentEnd(String params, int contentStart) {
        // 查找", nodeTitle="模式来确定content的结束位置
        String searchPattern = ", nodeTitle=";
        int searchIndex = contentStart;
        
        while (searchIndex < params.length()) {
            int foundIndex = params.indexOf(searchPattern, searchIndex);
            if (foundIndex == -1) {
                return -1;
            }
            
            // 检查这个位置是否真的是参数分隔符（不在引号内）
            String beforePattern = params.substring(contentStart, foundIndex);
            if (isValidContentEnd(beforePattern)) {
                return foundIndex;
            }
            
            searchIndex = foundIndex + 1;
        }
        
        return -1;
    }

    /**
     * 检查是否是有效的content结束位置
     */
    private boolean isValidContentEnd(String contentPart) {
        // 简单检查：如果引号数量是偶数，说明没有未闭合的引号
        int quoteCount = 0;
        for (char c : contentPart.toCharArray()) {
            if (c == '"') {
                quoteCount++;
            }
        }
        return quoteCount % 2 == 0;
    }

    /**
     * 简单的字符串追加方法（兼容现有代码）
     */
    public void append(String data) {
        if (data != null) {
            appendCozeWorkflowMessage(data);
        }
    }

    /**
     * 追加纯文本内容（不进行JSON解析）
     */
    public void appendSimpleContent(String content) {
        if (content != null) {
            contentBuilder.append(content);
        }
    }
}