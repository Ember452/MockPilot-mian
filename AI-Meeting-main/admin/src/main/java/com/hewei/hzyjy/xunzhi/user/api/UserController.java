/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hewei.hzyjy.xunzhi.user.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.bean.BeanUtil;
import com.hewei.hzyjy.xunzhi.auth.application.LoginSessionService;
import com.hewei.hzyjy.xunzhi.auth.application.PermissionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserLoginReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserPageReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserRegisterReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.resp.UserActualRespDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.resp.UserPageRespDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.resp.UserRespDTO;
import com.hewei.hzyjy.xunzhi.user.service.AdminPermissionService;
import com.hewei.hzyjy.xunzhi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * User management controller.
 */
@RestController
@RequestMapping("/api/xunzhi/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminPermissionService adminPermissionService;
    private final LoginSessionService loginSessionService;
    private final PermissionService permissionService;

    @GetMapping("/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }

    @GetMapping("/actual/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    @GetMapping("/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam,
                               @CurrentUser String currentUsername) {
        userService.update(requestParam, currentUsername);
        return Results.success();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody UserLoginReqDTO requestParam) {
        userService.login(requestParam);
        loginSessionService.login(requestParam.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", loginSessionService.getCurrentToken());
        result.put("username", requestParam.getUsername());
        result.put("isAdmin", permissionService.isAdmin(requestParam.getUsername()));
        return Results.success(result);
    }

    @GetMapping("/check-login")
    public Result<Map<String, Object>> checkLogin() {
        Map<String, Object> result = new HashMap<>();
        boolean isLogin = loginSessionService.isCurrentLoggedIn();
        result.put("isLogin", isLogin);
        if (isLogin) {
            result.put("username", loginSessionService.getCurrentLoginId());
            result.put("token", loginSessionService.getCurrentToken());
        }
        return Results.success(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        loginSessionService.logoutCurrent();
        return Results.success();
    }

    @GetMapping("/is-admin")
    public Result<Map<String, Object>> isAdmin() {
        Map<String, Object> result = new HashMap<>();
        if (loginSessionService.isCurrentLoggedIn()) {
            String username = loginSessionService.getCurrentLoginId();
            result.put("isAdmin", permissionService.isAdmin(username));
            result.put("username", username);
        } else {
            result.put("isAdmin", false);
        }
        return Results.success(result);
    }

    @PostMapping("/admin")
    @SaCheckRole("admin")
    public Result<Void> addAdmin(@RequestBody String username) {
        adminPermissionService.setAdminByUserId(username);
        return Results.success();
    }

    @GetMapping("/page")
    public Result<IPage<UserPageRespDTO>> pageUsers(UserPageReqDTO requestParam) {
        return Results.success(userService.pageUsers(requestParam));
    }
}
