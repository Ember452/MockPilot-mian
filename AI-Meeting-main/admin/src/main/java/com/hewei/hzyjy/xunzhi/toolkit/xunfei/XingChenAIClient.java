package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class XingChenAIClient {

    public void chat(String input,
                     String chatId,
                     String history,
                     boolean stream,
                     OutputStream outputStream,
                     Consumer<String> callback,
                     String customApiKey,
                     String customApiSecret,
                     String customFlowId) throws Exception {
        chat(input, chatId, history, stream, outputStream, callback,
                customApiKey, customApiSecret, customFlowId, null, null);
    }

    public void chat(String input,
                     String chatId,
                     String history,
                     boolean stream,
                     OutputStream outputStream,
                     Consumer<String> callback,
                     String customApiKey,
                     String customApiSecret,
                     String customFlowId,
                     Map<String, Object> extraParameters) throws Exception {
        chat(input, chatId, history, stream, outputStream, callback,
                customApiKey, customApiSecret, customFlowId, null, extraParameters);
    }

    public void chat(String input,
                     String chatId,
                     String history,
                     boolean stream,
                     OutputStream outputStream,
                     Consumer<String> callback,
                     String customApiKey,
                     String customApiSecret,
                     String customFlowId,
                     String fileUrl) throws Exception {
        chat(input, chatId, history, stream, outputStream, callback,
                customApiKey, customApiSecret, customFlowId, fileUrl, null);
    }

    public void chat(String input,
                     String chatId,
                     String history,
                     boolean stream,
                     OutputStream outputStream,
                     Consumer<String> callback,
                     String customApiKey,
                     String customApiSecret,
                     String customFlowId,
                     String fileUrl,
                     Map<String, Object> extraParameters) throws Exception {
        URL url = new URL("https://xingchen-api.xf-yun.com/workflow/v1/chat/completions");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Authorization", "Bearer " + customApiKey + ":" + customApiSecret);
        conn.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("flow_id", customFlowId);
        requestBody.put("uid", "123");
        requestBody.put("stream", stream);
        requestBody.put("chat_id", chatId);
        requestBody.put("history", parseHistory(history));
        requestBody.put("parameters", buildParameters(input, fileUrl, extraParameters));

        String payload = requestBody.toJSONString();
        log.debug("Dispatching XingChen chat request, chatId={}, stream={}, hasFile={}, extraParameterCount={}",
                chatId,
                stream,
                fileUrl != null && !fileUrl.trim().isEmpty(),
                extraParameters == null ? 0 : extraParameters.size());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        log.info("XingChen chat response code={}", responseCode);
        if (responseCode != HttpsURLConnection.HTTP_OK) {
            throw new IOException("XingChen chat request failed, status=" + responseCode
                    + ", body=" + readResponseBody(conn, true));
        }

        if (stream) {
            handleStreamingResponse(conn, chatId, outputStream, callback);
        } else {
            String response = readResponseBody(conn, false);
            callback.accept(response);
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        conn.disconnect();
    }

    public String uploadFile(MultipartFile file, String apiKey, String apiSecret) throws Exception {
        URL url = new URL("https://xingchen-api.xf-yun.com/workflow/v1/upload_file");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Host", "xingchen-api.xf-yun.com");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey + ":" + apiSecret);

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream outputStream = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

            writer.append("--").append(boundary).append("\r\n");
            String contentType = getContentTypeByFileName(file.getOriginalFilename());
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getOriginalFilename())
                    .append("\"; type=")
                    .append(contentType)
                    .append("\r\n");
            writer.append("Content-Type: ").append(contentType).append("\r\n");
            writer.append("\r\n");
            writer.flush();

            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            outputStream.flush();

            writer.append("\r\n");
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();
        }

        int responseCode = conn.getResponseCode();
        log.info("XingChen upload response code={}", responseCode);
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            String responseBody = readResponseBody(conn, false);
            log.info("XingChen upload response={}", responseBody);
            return parseFileUrlFromResponse(responseBody);
        }

        String errorBody = readResponseBody(conn, true);
        log.error("XingChen upload failed: {}", errorBody);
        throw new RuntimeException("File upload failed: " + errorBody);
    }

    private Object parseHistory(String history) {
        try {
            Object historyObj = JSON.parse(history);
            if (historyObj instanceof List<?>) {
                return historyObj;
            }
            log.warn("history payload is not a JSON array, fallback to empty list");
        } catch (Exception ex) {
            log.warn("Failed to parse history payload, fallback to empty list: {}", ex.getMessage());
        }
        return new ArrayList<>();
    }

    private JSONObject buildParameters(String input, String fileUrl, Map<String, Object> extraParameters) {
        JSONObject parameters = new JSONObject();
        parameters.put("AGENT_USER_INPUT", input);
        if (extraParameters != null && !extraParameters.isEmpty()) {
            for (Map.Entry<String, Object> entry : extraParameters.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    continue;
                }
                if (entry.getValue() != null) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            parameters.put("USER_FILE", fileUrl);
        }
        return parameters;
    }

    private void handleStreamingResponse(HttpsURLConnection conn,
                                         String chatId,
                                         OutputStream outputStream,
                                         Consumer<String> callback) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String processedLine = extractSsePayload(line);
                if (processedLine == null) {
                    log.debug("Skipped non-payload SSE line, chatId={}", chatId);
                    continue;
                }

                callback.accept(processedLine);
                log.debug("Received XingChen SSE chunk, chatId={}, payloadLength={}",
                        chatId, processedLine.length());
                outputStream.write(processedLine.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }
    }

    private String extractSsePayload(String line) {
        if (line.startsWith("data: ")) {
            String dataContent = line.substring(6).trim();
            if (dataContent.startsWith("{") || dataContent.equals("[DONE]")) {
                return dataContent;
            }
            return null;
        }
        if (line.startsWith("{") || line.equals("[DONE]")) {
            return line;
        }
        return null;
    }

    private String readResponseBody(HttpsURLConnection conn, boolean errorStream) throws IOException {
        InputStream stream = errorStream ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String getContentTypeByFileName(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerFileName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private String parseFileUrlFromResponse(String responseBody) {
        try {
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            Integer code = jsonResponse.getInteger("code");
            if (code == null || code != 0) {
                String message = jsonResponse.getString("message");
                throw new RuntimeException("File upload failed, code=" + code + ", message=" + message);
            }

            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                throw new RuntimeException("Upload response missing data field: " + responseBody);
            }

            String url = data.getString("url");
            if (url == null || url.trim().isEmpty()) {
                throw new RuntimeException("Upload response missing url field: " + responseBody);
            }

            log.info("Parsed XingChen file URL={}", url);
            return url;
        } catch (Exception ex) {
            log.error("Failed to parse XingChen file URL, response={}", responseBody, ex);
            throw new RuntimeException("Failed to parse file URL: " + ex.getMessage());
        }
    }
}
