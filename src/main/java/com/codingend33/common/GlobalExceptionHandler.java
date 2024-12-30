package com.codingend33.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

//运用了AOP和动态代理的的理念，增强controller方法，在controller报异常时就使用这个全局异常处理方法。
//@RestControllerAdvice //表示当前类为全局异常处理器【相当于切面类，为Rest风格开发的控制器类做增强】
//@ExceptionHandler //指定可以捕获哪种类型的异常进行处理。【相当于切入点表达式。控制器方法都用于那些异常类型】

@Slf4j
// 代表我们定义了一个全局异常处理器，可以捕获controller抛出的异常。
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody：
// ControllerAdvice【拦截所有控制器以捕获异常】 + responsebody【响应给浏览器json格式数据】
@RestControllerAdvice
public class GlobalExceptionHandler {

    //捕获重复命名的异常
    //ExceptionHandler注解当中的value属性来指定我们要捕获的是哪一类型的异常
    //SQLIntegrityConstraintViolationException.class，这是重名时会报的异常。
    //返回值类型是string,因为只是返回错误信息的字符串，不需要data。
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException exception) {

        // 控制台输出错误信息， 输出的结果类似于：Duplicate entry 'zhangsan' for key 'employee.idx_username'
        log.error(exception.getMessage());

        //根据上面的输出结果，我们可以判断信息中如果包含Duplicate entry，则说明有条目重复
        //可以提取出重复的用户名，然后自定义异常输出的格式。
        if (exception.getMessage().contains("Duplicate entry")) {
            //用split()方法来对错误信息拆分为数组
            String[] split = exception.getMessage().split(" ");
            //字符串格式是固定的，所以这个索引2的位置必然是username
            String msg = split[2];
            //拼串成自定义的错误信息
            return R.error("用户名" + msg + "已存在");
        }
        //如果是别的错误那我也没招儿了
        return R.error("未知错误");
    }

    //捕获自定义异常类
    //CustomException是自定义的类。
    //当抛出 CustomException 类型的异常时，Spring 将创建的异常对象 CustomException 注入到 exceptionHandler 方法中。不会让它继续向上冒泡。
    //比如，当业务逻辑出现异常时 操作：throw new CustomException("员工不存在") ，创建一个 CustomException 对象，
    //CustomException类中定义了一个构造方法，使用的父类的构造方法，异常信息存储到父类中
    //可以使用getMessage获取存放的异常信息。
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException exception) {
        log.error(exception.getMessage());
        //构建一个统一格式的响应对象，表示发生了错误，并携带异常信息，响应到浏览器后通过前端逻辑会显示异常信息。
        return R.error(exception.getMessage());
    }
}
