package com.hewei.hzyjy.xunzhi.common.convention.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 分页信息工具类
 */
@Data
public class PageInfo<T> {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页数量
     */
    private Long size = 10L;

    /**
     * 总记录数
     */
    private Long total = 0L;

    /**
     * 总页数
     */
    private Long pages = 0L;

    /**
     * 分页数据
     */
    private List<T> records;

    public PageInfo() {
    }

    public PageInfo(Long current, Long size) {
        this.current = current;
        this.size = size;
    }

    public PageInfo(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records;
        this.pages = total % size == 0 ? total / size : total / size + 1;
    }

    /**
     * 将MyBatis-Plus的Page对象转换为PageInfo对象
     */
    public static <T> PageInfo<T> of(Page<T> page) {
        return new PageInfo<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    /**
     * 构建空分页对象
     */
    public static <T> PageInfo<T> empty() {
        return new PageInfo<>();
    }

    /**
     * 构建分页对象
     */
    public static <T> PageInfo<T> of(long current, long size) {
        return new PageInfo<>(current, size);
    }
}