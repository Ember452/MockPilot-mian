package com.hewei.hzyjy.xunzhi.user.api.io.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hewei.hzyjy.xunzhi.common.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 用户分页查询响应DTO
 */
@Data
public class UserPageRespDTO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 手机号（脱敏）
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;
    
    /**
     * 邮箱
     */
    private String mail;
    
    /**
     * 用户状态：0-正常，1-禁用
     */
    private Integer delFlag;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    
    /**
     * 是否为管理员
     */
    private Boolean isAdmin;
}