package com.codingend33.common;

//自定义异常类
//继承了 Java 的 RuntimeException 类，表示这是一个运行时异常。
//比如，当业务逻辑出现异常时 操作：throw new CustomException("员工不存在") 创建一个 CustomException 对象，
// 并将异常信息 "员工不存在" 传入其构造方法。

public class CustomException extends RuntimeException{

    //定义了一个构造方法，接收一个字符串参数 msg。msg 是异常信息，用于描述发生异常的原因。
    public CustomException(String msg){
        //使用 super(msg) 调用父类 RuntimeException 的构造方法，将异常信息存储到父类中。
        //父类 RuntimeException 会将 msg 作为异常信息保存起来，这样当异常被捕获时，可以通过 getMessage() 方法获取 msg。
        //如果不调用父类构造方法，RuntimeException 将无法存储你的 msg 信息。
        //因为RuntimeException 的 message 字段是私有的，子类无法直接访问或修改
        //通过调用父类的构造方法，可以间接为 message 字段赋值。
        super(msg);
    }
}
