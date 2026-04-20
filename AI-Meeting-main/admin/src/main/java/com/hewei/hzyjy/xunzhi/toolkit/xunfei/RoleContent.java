package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import lombok.Data; /**
     * 角色内容类
     */
    @Data
    public class RoleContent {
        private String role;
        private String content;
        
        public RoleContent() {}
        
        public RoleContent(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
