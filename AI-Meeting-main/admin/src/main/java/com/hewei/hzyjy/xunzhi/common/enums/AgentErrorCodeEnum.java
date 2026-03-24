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

package com.hewei.hzyjy.xunzhi.common.enums;

import com.hewei.hzyjy.xunzhi.common.convention.errorcode.IErrorCode;

/**
 * 智能体错误码
 
 */
public enum AgentErrorCodeEnum implements IErrorCode {

    Agent_NULL("B000300", "智能体配置不存在"),

    AGENT_NAME_EXIST("B000301", "智能体已存在"),

    AGENT_EXIST("B000302", "智能体记录已存在"),

    AGENT_SAVE_ERROR("B000303", "智能体记录新增失败"),

    DEMEANOR_EVALUATION_FAILED("B000304", "神态评分失败"),

    DEMEANOR_IMAGE_UPLOAD_FAILED("B000305", "神态评分图片上传失败"),

    DEMEANOR_AI_RESPONSE_PARSE_FAILED("B000306", "神态评分AI响应解析失败"),

    DEMEANOR_SCORE_INVALID("B000307", "神态评分数据无效");

    private final String code;

    private final String message;

    AgentErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
