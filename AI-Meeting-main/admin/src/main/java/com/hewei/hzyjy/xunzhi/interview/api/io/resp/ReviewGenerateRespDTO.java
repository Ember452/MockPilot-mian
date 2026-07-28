package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 复习清单生成响应DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewGenerateRespDTO {

    /**
     * 本次是否新生成（false=此前已生成，幂等返回）
     */
    private Boolean generated;

    /**
     * 该会话下的复习条目总数
     */
    private Integer itemCount;

    /**
     * 幂等返回时为无参考片段的存量条目补充检索的条数（新生成时恒为 0）
     */
    private Integer refsBackfilled;
}
