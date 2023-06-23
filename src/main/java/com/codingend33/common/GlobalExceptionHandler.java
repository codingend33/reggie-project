package com.codingend33.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //指定要处理的异常为SQLIntegrityConstraintViolationException.class，这是重名报的异常。
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException exception) {
        // 控制台输出错误信息
        log.error(exception.getMessage());

        //如果包含Duplicate entry，则说明有条目重复
        if (exception.getMessage().contains("Duplicate entry")) {
            //对字符串切片，根据空格分隔
            String[] split = exception.getMessage().split(" ");
            //字符串格式是固定的，所以这个索引2的位置必然是username
            String msg = split[2];
            //拼串作为错误信息返回
            return R.error("用户名" + msg + "已存在");
        }
        //如果是别的错误那我也没招儿了
        return R.error("未知错误");
    }

    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException exception) {
        log.error(exception.getMessage());
        return R.error(exception.getMessage());
    }



}
