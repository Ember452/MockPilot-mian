package com.hewei.hzyjy.xunzhi.media.api.io.req;

import lombok.Data;

/**
 * Request payload for long-text TTS.
 */
@Data
public class LongTextTtsReqDTO {

    /**
     * Text to synthesize.
     */
    private String text;

    /**
     * Voice name, default `x4_mingge`.
     */
    private String vcn;

    /**
     * Language code, default `zh`.
     */
    private String language;

    /**
     * Speech speed in range `[0, 100]`.
     */
    private Integer speed;

    /**
     * Volume in range `[0, 100]`.
     */
    private Integer volume;

    /**
     * Pitch in range `[0, 100]`.
     */
    private Integer pitch;

    /**
     * Whether to return pinyin content, `0` or `1`.
     */
    private Integer rhy;

    /**
     * Audio encoding, default `lame` (mp3).
     */
    private String audioEncoding;

    /**
     * Sample rate, default `16000`.
     */
    private Integer sampleRate;

    /**
     * Timeout in seconds when waiting synchronously.
     */
    private Integer timeoutSeconds;

    /**
     * Polling interval in milliseconds.
     */
    private Integer pollIntervalMs;
}
