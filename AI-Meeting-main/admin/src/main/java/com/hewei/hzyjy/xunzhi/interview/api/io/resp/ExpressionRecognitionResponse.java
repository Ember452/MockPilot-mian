package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;
import java.util.List;

/**
 * 表情识别响应实体类
 */
@Data
public class ExpressionRecognitionResponse {
    private Integer code;
    private ExpressionData data;
    private String desc;
    private String sid;

    @Data
    public static class ExpressionData {
        private List<FileResult> fileList;
        private Integer reviewCount;
        private List<Integer> statistic;
    }

    @Data
    public static class FileResult {
        private Integer label;
        private String name;
        private Double rate;
        private Boolean review;
    }
}