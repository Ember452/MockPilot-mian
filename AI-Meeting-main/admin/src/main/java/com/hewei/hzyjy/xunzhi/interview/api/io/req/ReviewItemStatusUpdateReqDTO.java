package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复习条目状态更新请求DTO
 */
@Data
public class ReviewItemStatusUpdateReqDTO {

    /**
     * 目标状态：0=待复习，1=已掌握
     */
    @NotNull(message = "status不能为空")
    @Min(value = 0, message = "status非法")
    @Max(value = 1, message = "status非法")
    private Integer status;
}
