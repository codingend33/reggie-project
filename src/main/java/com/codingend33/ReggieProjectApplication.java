package com.codingend33;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;



@Slf4j //可以使用log.info直接在控制台输出日志。
@SpringBootApplication //springboot启动入口
@ServletComponentScan //扫描过滤器
@EnableTransactionManagement //启用事务支持
@EnableCaching //开启缓存注解功能
public class ReggieProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieProjectApplication.class, args);
        log.info("项目启动成功");
    }

}
