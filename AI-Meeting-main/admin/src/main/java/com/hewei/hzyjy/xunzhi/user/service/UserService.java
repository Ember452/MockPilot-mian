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

package com.hewei.hzyjy.xunzhi.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.user.dao.entity.UserDO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserLoginReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserPageReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserRegisterReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.resp.UserLoginRespDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.resp.UserPageRespDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.resp.UserRespDTO;

/**
 * 用户接口层
 **/
public interface UserService extends IService<UserDO> {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否存在
     *
     * @param username 用户名
     * @return 用户名存在返回 True，不存在返回 False
     */
    Boolean hasUsername(String username);

    /**
     * 注册用户
     *
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户
     *
     * @param requestParam 修改用户请求参数
     */
    void update(UserUpdateReqDTO requestParam, String currentUsername);

    /**
     * 用户登录
     *
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数 Token
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * 检查用户是否登录
     *
     * @param username 用户名
     * @param token    用户登录 Token
     * @return 用户是否登录标识
     */
    Boolean checkLogin(String username, String token);

    /**
     * 退出登录
     *
     * @param username 用户名
     * @param token    用户登录 Token
     */
    void logout(String username, String token);

    IPage<UserPageRespDTO> pageUsers(UserPageReqDTO requestParam);
}

