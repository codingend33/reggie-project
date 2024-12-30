package com.codingend33.config;

import com.codingend33.common.JacksonObjectMapper;
import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

/**
 * 配置类:完成资源映射，消息转换
 */

@Configuration //配置类需要添加这个注解
@Slf4j //日志输出
@EnableSwagger2
@EnableKnife4j
//需要继承mvc的一个接口，并重写addresourcehandlers方法
public class WebMvcConfig extends WebMvcConfigurationSupport {

    //如果静态资源直接放在resources目录下，则需要配置一下资源映射
    //前端请求的包含backend和front目录的文件，直接映射到路径/backend/和/front/
    //addResourceHandler是控制请求
    //addResourceLocations是映射到路径
    //Spring MVC 提供的资源处理器配置，用于将特定的 URL 请求映射到静态资源文件夹。
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


    //扩展Spring mvc的消息转换器，允许用户在默认的消息转换器基础上添加或修改自己的消息转换器。
    //使用JacksonobjectMapper对象转换器进行Java对象到json数据的转换。
    //  流程：
    //  Spring MVC 在初始化时，会加载默认的消息转换器列表。
    //  extendMessageConverters 方法被调用，可以在默认的转换器基础上扩展或调整。
    //  新创建的 MappingJackson2HttpMessageConverter 配置了自定义的 ObjectMapper。
    //  将这个转换器添加到转换器列表的首位，确保它的优先级最高。
    //  当 MVC 框架需要将 Java 对象转换为 JSON 或解析 JSON 时，会优先使用这个自定义的消息转换器。
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

        //MappingJackson2HttpMessageConverter 是 Spring 提供的一个消息转换器，用于将 Java 对象与 JSON 数据之间相互转换。
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转化器，底层使用jackson将java对象转为json
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        //通过对 converters 的操作，可以添加新的转换器或调整现有转换器的优先级。
        //将上面的消息转换器对象追加到mvc框架的默认转换器集合当中
        //index设置为0，将自定义转换器放在列表的第一个位置，确保它的优先级最高。如果其他转换器也可以处理同样的类型，自定义转换器会优先使用。
        converters.add(0, messageConverter);
    }

    @Bean
    public Docket createRestApi() {
        //文档类型
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.codingend33.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    //描述API的基础信息
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("瑞吉外卖")
                .version("1.0")
                .description("瑞吉外卖接口文档")
                .build();
    }

}

