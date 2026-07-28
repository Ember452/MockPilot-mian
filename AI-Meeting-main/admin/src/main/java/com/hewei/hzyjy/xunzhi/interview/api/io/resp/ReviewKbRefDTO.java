package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

/**
 * 复习条目关联的知识库参考片段。
 */
@Data
public class ReviewKbRefDTO {

    private String fileName;

    private String docId;

    /**
     * 命中片段摘要（截断后的正文）。
     */
    private String snippet;
}
