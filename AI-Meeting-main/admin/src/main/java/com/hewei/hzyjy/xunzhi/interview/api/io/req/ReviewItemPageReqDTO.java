package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import lombok.Data;

/**
 * 复习清单分页查询请求DTO
 */
@Data
public class ReviewItemPageReqDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 状态筛选（可选）：0=待复习，1=已掌握
     */
    private Integer status;
}
