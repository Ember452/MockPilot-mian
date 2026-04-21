package com.hewei.hzyjy.xunzhi.interview.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.context.UserContext;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewRecordPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewRecordSaveReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewRecordRespDTO;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/xunzhi/v1/interview")
@RequiredArgsConstructor
public class InterviewRecordController {

    private final InterviewRecordService interviewRecordService;

    @PostMapping("/interview/record")
    public Result<Void> saveInterviewRecord(
            @Valid @RequestBody InterviewRecordSaveReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        interviewRecordService.saveInterviewRecord(requestParam.getSessionId(), currentUser.getUserId(), requestParam);
        return Results.success();
    }

    @GetMapping("/interview/records")
    public Result<IPage<InterviewRecordRespDTO>> pageInterviewRecords(
            InterviewRecordPageReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewRecordService.pageInterviewRecords(currentUser.getUserId(), requestParam));
    }

    @GetMapping("/interview/record/{sessionId}")
    public Result<InterviewRecordRespDTO> getInterviewRecordBySessionId(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewRecordService.getBySessionId(sessionId, currentUser.getUserId()));
    }

    @PostMapping("/interview/record/save-from-redis/{sessionId}")
    public Result<Void> saveInterviewRecordFromRedis(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        interviewRecordService.saveInterviewRecordFromRedis(sessionId, currentUser.getUserId());
        return Results.success();
    }

    @DeleteMapping("/interview/record/{sessionId}")
    public Result<Void> deleteInterviewRecord(@PathVariable String sessionId,
                                              @CurrentUser UserContext currentUser) {
        interviewRecordService.deleteInterviewRecord(sessionId, currentUser.getUserId());
        return Results.success();
    }

    @DeleteMapping("/interview/records")
    public Result<Void> clearAllInterviewRecords(@CurrentUser UserContext currentUser) {
        interviewRecordService.clearAllInterviewRecords(currentUser.getUserId());
        return Results.success();
    }
}
