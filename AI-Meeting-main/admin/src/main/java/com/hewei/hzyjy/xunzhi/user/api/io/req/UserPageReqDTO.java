package com.hewei.hzyjy.xunzhi.user.api.io.req;

import lombok.Data;

/**
 * 用户分页查询请求DTO
 */
@Data
public class UserPageReqDTO {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 搜索关键词（可选）- 支持用户名、真实姓名模糊搜索
     */
    private String keyword;
    
    /**
     * 用户状态（可选）：0-正常，1-禁用
     */
    private Integer status;
    
    /**
     * 创建时间排序（可选）：asc-升序，desc-降序
     */
    private String createTimeSort = "desc";
}