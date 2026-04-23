package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 雷达图维度项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RadarDimensionItemRespDTO {

    /**
     * 维度键（英文，前端可用于标识）。
     */
    private String key;

    /**
     * 维度名称（中文展示）。
     */
    private String label;

    /**
     * 分值（0-100）。
     */
    private Integer score;
}
