package com.hewei.hzyjy.xunzhi.knowledge.flow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询改写启发式跳过判定测试。
 */
class QueryRewriteNodeTest {

    @Test
    void selfContainedLongQuerySkipsRewrite() {
        assertTrue(QueryRewriteNode.isSelfContained("Elasticsearch如何配置ik分词器实现中文检索"));
    }

    @Test
    void anaphoricQueryNeedsRewrite() {
        assertFalse(QueryRewriteNode.isSelfContained("那它的缺点是什么呢请详细说明"));
        assertFalse(QueryRewriteNode.isSelfContained("上面提到的方案具体怎么实施落地"));
    }

    @Test
    void shortQueryNeedsRewrite() {
        assertFalse(QueryRewriteNode.isSelfContained("缺点呢"));
    }

    @Test
    void nullQueryNeedsRewrite() {
        assertFalse(QueryRewriteNode.isSelfContained(null));
    }
}
