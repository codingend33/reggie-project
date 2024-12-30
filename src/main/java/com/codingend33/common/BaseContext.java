package com.codingend33.common;

/**
 * 用于在多线程环境中存储和获取当前线程的用户信息（如用户 ID）。
 * 它使用了 Java 的 ThreadLocal 机制，为每个线程提供独立的变量副本，保证线程之间的数据隔离。
 */
public class BaseContext {

    //封装一个静态的局部线程对象，泛型就是Long，因为要存储的ID类型就是Long.
    //每个线程会拥有自己的 threadLocal 变量，不会互相干扰。
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 设置当前线程的 ID，没有返回值类型
    public static void setCurrentId(Long id) {
    // 将 ID 存储到当前线程的 ThreadLocal局部对象中
        threadLocal.set(id);
    }

    //从当前线程的 ThreadLocal局部对象，中获取存储的用户 ID，有返回值，就是Long
    public static Long getCurrentId() {
        return threadLocal.get();
    }
}
