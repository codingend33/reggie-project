package com.codingend33.common;

public class BaseContext {

    // 封装一个静态的局部线程对象，泛型就是Long，因为要存储的ID，ID的类型就是Long.
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 给线程存放ID，没有返回值类型
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    // 获取线程中的ID，有返回值，就是Long
    public static Long getCurrentId() {
        return threadLocal.get();
    }
}
