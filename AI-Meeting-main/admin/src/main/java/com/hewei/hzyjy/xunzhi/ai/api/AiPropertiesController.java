package com.hewei.hzyjy.xunzhi.ai.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesCreateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesPageReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiModelOptionRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiPropertiesRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/xunzhi/v1/ai-properties")
@RequiredArgsConstructor
public class AiPropertiesController {

    private final AiPropertiesService aiPropertiesService;

    /**
     * 获取所有可用AI模型（用于前端下拉列表）
     */
    @GetMapping("/options")
    public Result<List<AiModelOptionRespDTO>> getAvailableAiModels(@CurrentUser String username) {
        return Results.success(aiPropertiesService.getAvailableAiModels(username));
    }

    @PostMapping
    public Result<Void> createAiProperties(@RequestBody AiPropertiesCreateReqDTO requestParam, @CurrentUser String username) {
        aiPropertiesService.createAiProperties(requestParam, username);
        return Results.success();
    }

    /**
     * 测试连接：按请求体中的配置发一次最小聊天请求验证 Key 可用性
     */
    @PostMapping("/test")
    public Result<String> testConnection(@RequestBody AiPropertiesCreateReqDTO requestParam) {
        return Results.success(aiPropertiesService.testConnection(requestParam));
    }

    @PutMapping
    public Result<Void> updateAiProperties(@RequestBody AiPropertiesUpdateReqDTO requestParam, @CurrentUser String username) {
        aiPropertiesService.updateAiProperties(requestParam, username);
        return Results.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAiProperties(@PathVariable Long id, @CurrentUser String username) {
        aiPropertiesService.deleteAiProperties(id, username);
        return Results.success();
    }

    @GetMapping("/{id}")
    public Result<AiPropertiesRespDTO> getAiPropertiesById(@PathVariable Long id, @CurrentUser String username) {
        AiPropertiesRespDTO result = aiPropertiesService.getAiPropertiesById(id, username);
        return Results.success(result);
    }

    @GetMapping
    public Result<IPage<AiPropertiesRespDTO>> pageAiProperties(AiPropertiesPageReqDTO requestParam, @CurrentUser String username) {
        IPage<AiPropertiesRespDTO> result = aiPropertiesService.pageAiProperties(requestParam, username);
        return Results.success(result);
    }

    @GetMapping("/enabled")
    public Result<List<AiPropertiesRespDTO>> getAllEnabledAiProperties(@CurrentUser String username) {
        List<AiPropertiesRespDTO> result = aiPropertiesService.getAllEnabledAiProperties(username);
        return Results.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleAiPropertiesStatus(@PathVariable Long id, @RequestParam Integer isEnabled, @CurrentUser String username) {
        aiPropertiesService.toggleAiPropertiesStatus(id, isEnabled, username);
        return Results.success();
    }
}
