package com.hewei.hzyjy.xunzhi.interview.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.context.UserContext;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.ReviewItemPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.ReviewItemStatusUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGenerateRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGrowthRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewItemRespDTO;
import com.hewei.hzyjy.xunzhi.interview.service.ReviewItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 复习闭环接口：生成复习清单 / 条目管理 / 成长曲线。
 */
@RestController
@RequestMapping("/api/xunzhi/v1/interview")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewItemService reviewItemService;

    @PostMapping("/review/generate/{sessionId}")
    public Result<ReviewGenerateRespDTO> generate(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long kbId,
            @CurrentUser UserContext currentUser) {
        return Results.success(reviewItemService.generateFromInterview(
                sessionId, currentUser.getUserId(), currentUser.getUsername(), kbId));
    }

    @GetMapping("/review/items")
    public Result<IPage<ReviewItemRespDTO>> pageItems(
            ReviewItemPageReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        return Results.success(reviewItemService.pageReviewItems(currentUser.getUserId(), requestParam));
    }

    @PatchMapping("/review/items/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReviewItemStatusUpdateReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        reviewItemService.updateStatus(currentUser.getUserId(), id, requestParam.getStatus());
        return Results.success();
    }

    @GetMapping("/review/growth")
    public Result<ReviewGrowthRespDTO> growth(@CurrentUser UserContext currentUser) {
        return Results.success(reviewItemService.growth(currentUser.getUserId()));
    }
}
