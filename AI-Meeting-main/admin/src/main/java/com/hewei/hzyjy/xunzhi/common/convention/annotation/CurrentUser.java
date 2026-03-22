package com.hewei.hzyjy.xunzhi.common.convention.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入当前登录用户信息
 * 支持注入类型：
 * 1. String: 获取用户名
 * 2. Long: 获取用户ID
 * 3. UserContext: 获取完整用户上下文
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
