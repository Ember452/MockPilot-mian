package com.hewei.hzyjy.xunzhi.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Current authenticated principal resolved from the active token context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentPrincipal {

    private Long userId;

    private String username;
}
