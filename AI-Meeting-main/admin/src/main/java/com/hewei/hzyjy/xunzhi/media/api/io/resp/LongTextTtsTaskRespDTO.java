package com.hewei.hzyjy.xunzhi.media.api.io.resp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * Response payload for long-text TTS tasks.
 */
@Data
public class LongTextTtsTaskRespDTO {

    /**
     * Xunfei session id.
     */
    private String sid;

    /**
     * Task id.
     */
    private String taskId;

    /**
     * Task status.
     * 1: task created
     * 2: task creation failed
     * 3: task running
     * 4: task execution failed
     * 5: task completed
     */
    private String taskStatus;

    /**
     * Platform status code, `0` means success.
     */
    private Integer code;

    /**
     * Platform message.
     */
    private String message;

    /**
     * Download URL for the synthesized audio after decoding.
     */
    @JsonIgnore
    private String audioUrl;

    /**
     * Synthesized audio content encoded as Base64.
     */
    private String audioBase64;

    /**
     * Download URL for pinyin content after decoding.
     */
    @JsonIgnore
    private String pybufUrl;

    /**
     * Pinyin content text.
     */
    private String pybufContent;

    /**
     * Whether the task has completed.
     */
    private Boolean completed;

    /**
     * Whether the task succeeded.
     */
    private Boolean success;
}
