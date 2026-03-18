
package com.hewei.hzyjy.xunzhi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.hewei.hzyjy.xunzhi.**.dao.mapper")
@EnableScheduling
@EnableAsync
public class XunZhiAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(XunZhiAdminApplication.class, args);
    }
}
