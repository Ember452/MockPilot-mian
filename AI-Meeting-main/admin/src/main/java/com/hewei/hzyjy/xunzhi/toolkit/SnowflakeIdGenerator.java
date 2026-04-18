package com.hewei.hzyjy.xunzhi.toolkit;

import org.springframework.stereotype.Component;

/**
 * 基于雪花算法的全局ID生成器
 */
@Component
public class SnowflakeIdGenerator {
    // 开始时间戳（2024-01-01 00:00:00）
    private final long START_TIMESTAMP = 1704038400000L;
    
    // 机器ID所占位数
    private final long WORKER_ID_BITS = 5L;
    // 数据中心ID所占位数
    private final long DATACENTER_ID_BITS = 5L;
    // 序列号所占位数
    private final long SEQUENCE_BITS = 12L;
    
    // 支持的最大机器ID
    private final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 支持的最大数据中心ID
    private final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    
    // 机器ID向左移12位
    private final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    // 数据中心ID向左移17位(12+5)
    private final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳向左移22位(12+5+5)
    private final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    
    // 序列号掩码，用于限制序列号的最大值
    private final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    // 工作机器ID(0~31)
    private long workerId = 0L;
    // 数据中心ID(0~31)
    private long datacenterId = 0L;
    // 毫秒内序列号(0~4095)
    private long sequence = 0L;
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator() {
        // 默认使用0号机器，0号数据中心
        this(0L, 0L);
    }
    
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        // 检查机器ID和数据中心ID是否超出范围
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + MAX_WORKER_ID + " or less than 0");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID can't be greater than " + MAX_DATACENTER_ID + " or less than 0");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    /**
     * 生成全局唯一ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + " milliseconds");
        }
        
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }
        
        // 上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    /**
     * 返回以毫秒为单位的当前时间
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}