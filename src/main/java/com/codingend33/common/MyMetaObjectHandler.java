package com.codingend33.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.fasterxml.jackson.databind.ser.Serializers;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    // metaObject作为形参，MP给它封装了前端发送的请求信息。公共字段的值会由下面具体的方法中自动添加。
    // 如比插入新的员工王五，那所有发送请求的信息会自动封装到metaObject中，作为实参进入insertFill方法
    // 然后再为王五设定各种需要的公共字段。

    //插入时自动填充
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充【insert】。。。");
        log.info(metaObject.toString());
        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());
        //原来都是通过session获取当前登录者的ID，但是当前的类中无法获取session,所以现在先写死一个值，后面再优化
        metaObject.setValue("createUser", BaseContext.getCurrentId());
        metaObject.setValue("updateUser", BaseContext.getCurrentId());
    }


    //更新时自动填充
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充【update】。。。");
        log.info(metaObject.toString());
/*        //获取线程ID 验证属于同一个线程
        long id = Thread.currentThread().getId() ;
        log.info("线程id:{}" ,id);*/
        metaObject.setValue("updateTime",LocalDateTime.now());
        metaObject.setValue("updateUser",BaseContext.getCurrentId());
    }
}
