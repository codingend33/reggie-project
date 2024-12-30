package com.codingend33.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MP分页插件
 * Spring 启动时，扫描到 MybatisPlusConfig 类。
 * 调用 mybatisPlusInterceptor() 方法。
 * 创建并配置拦截器 MybatisPlusInterceptor，添加分页功能。
 * 将 MybatisPlusInterceptor 对象注册为 Spring 的 Bean。
 * MyBatis Plus 自动加载该拦截器，实现分页功能。
 */

//配置类的注解
@Configuration
public class MybatisPlusConfig {

    //@Bean 注解：将方法的返回值注册为 Spring 容器中的一个 Bean
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        //创建拦截器对象
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        //添加分页拦截器
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        return mybatisPlusInterceptor;
    }
}

