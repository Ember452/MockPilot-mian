package com.hewei.hzyjy.xunzhi.knowledge.flow;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

@Slf4j
@LiteflowComponent("webSearch")
@RequiredArgsConstructor
public class WebSearchNode extends NodeComponent {

    private static final String SEARCH_API = "https://html.duckduckgo.com/html/?q=";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    @Override
    public void process() throws Exception {
        RagContext ctx = this.getContextBean(RagContext.class);

        // CRAG：仅在检索质量评估判定不合格时降级联网
        if (!ctx.isEnableWebSearch() || !ctx.isNeedWebSearch()) {
            return;
        }

        String query = ctx.getQuery();
        String result = performWebSearch(query);
        ctx.setWebSearchResult(result);
        log.info("Web search: query={}, result_length={}", query, result != null ? result.length() : 0);
    }

    private String performWebSearch(String query) {
        try {
            String url = SEARCH_API + java.net.URLEncoder.encode(query, "UTF-8");
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
            if (response.body() != null) {
                String html = response.body().string();
                return extractTextFromHtml(html);
            }
        } catch (Exception e) {
            log.warn("Web search failed for query={}: {}", query, e.getMessage());
        }
        return null;
    }

    private String extractTextFromHtml(String html) {
        String text = html.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("\\s+", " ");
        text = text.replaceAll("&[a-z]+;", " ");
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        return text.trim();
    }
}
