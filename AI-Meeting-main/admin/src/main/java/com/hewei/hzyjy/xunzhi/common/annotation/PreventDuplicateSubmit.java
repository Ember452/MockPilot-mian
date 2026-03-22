package com.hewei.hzyjy.xunzhi.common.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重提交注解
 * 基于Redisson实现分布式锁，防止重复提交
 * 
 * @author nageoffer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicateSubmit {
    
    /**
     * 锁的前缀
     */
    String prefix() default "duplicate_submit";
    
    /**
     * 锁的过期时间（秒）
     */
    int expireTime() default 10;
    
    /**
     * 获取锁的等待时间（秒）
     */
    int waitTime() default 0;
    
    /**
     * 错误提示信息
     */
    String message() default "请勿重复提交";
    
    /**
     * 是否基于用户维度加锁
     */
    boolean userLevel() default true;
    
    /**
     * 是否基于会话维度加锁
     */
    boolean sessionLevel() default true;
    
    /**
     * 是否基于消息序号维度加锁
     */
    boolean messageSeqLevel() default true;
}