package com.hewei.hzyjy.xunzhi.ai.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
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
    public Result<List<AiModelOptionRespDTO>> getAvailableAiModels() {
        return Results.success(aiPropertiesService.getAvailableAiModels());
    }

    @PostMapping
    public Result<Void> createAiProperties(@RequestBody AiPropertiesCreateReqDTO requestParam) {
        aiPropertiesService.createAiProperties(requestParam);
        return Results.success();
    }

    @PutMapping
    public Result<Void> updateAiProperties(@RequestBody AiPropertiesUpdateReqDTO requestParam) {
        aiPropertiesService.updateAiProperties(requestParam);
        return Results.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAiProperties(@PathVariable Long id) {
        aiPropertiesService.deleteAiProperties(id);
        return Results.success();
    }

    @GetMapping("/{id}")
    public Result<AiPropertiesRespDTO> getAiPropertiesById(@PathVariable Long id) {
        AiPropertiesRespDTO result = aiPropertiesService.getAiPropertiesById(id);
        return Results.success(result);
    }

    @GetMapping
    public Result<IPage<AiPropertiesRespDTO>> pageAiProperties(AiPropertiesPageReqDTO requestParam) {
        IPage<AiPropertiesRespDTO> result = aiPropertiesService.pageAiProperties(requestParam);
        return Results.success(result);
    }

    @GetMapping("/enabled")
    public Result<List<AiPropertiesRespDTO>> getAllEnabledAiProperties() {
        List<AiPropertiesRespDTO> result = aiPropertiesService.getAllEnabledAiProperties();
        return Results.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleAiPropertiesStatus(@PathVariable Long id, @RequestParam Integer isEnabled) {
        aiPropertiesService.toggleAiPropertiesStatus(id, isEnabled);
        return Results.success();
    }
}
