package com.hewei.hzyjy.xunzhi.ai.api;

import com.hewei.hzyjy.xunzhi.ai.api.io.req.UserModelPreferenceSaveReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.UserModelPreferenceRespDTO;
import com.hewei.hzyjy.xunzhi.ai.service.UserModelPreferenceService;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户功能级默认模型绑定接口
 */
@RestController
@RequestMapping("/api/xunzhi/v1/user-model-preferences")
@RequiredArgsConstructor
public class UserModelPreferenceController {

    private final UserModelPreferenceService userModelPreferenceService;

    @GetMapping
    public Result<List<UserModelPreferenceRespDTO>> listPreferences(@CurrentUser String username) {
        return Results.success(userModelPreferenceService.listPreferences(username));
    }

    @PutMapping
    public Result<Void> savePreference(@RequestBody UserModelPreferenceSaveReqDTO requestParam,
                                       @CurrentUser String username) {
        userModelPreferenceService.savePreference(username, requestParam.getFeatureCode(), requestParam.getAiId());
        return Results.success();
    }
}
