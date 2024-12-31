package com.codingend33.common;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 在开发 RESTful 接口时，直接返回数据（比如 String、List 等）有时不足以满足需求。通常，接口需要包含一些额外的信息，例如：
 * 状态码（表示请求成功还是失败）
 * 消息（错误或成功的提示信息）
 * 数据（返回的具体内容）
 * 为此，通常会定义一个统一的返回类，例如 R<T>。
 *
 * 在方法得到返回值时，先封装成R类型的对象，增加额外的信息，再响应给浏览器。
 *
 *  实现Serializable接口：
 * 因为Spring会先将对象序列化再存入Redis，但我们对返回值进行了统一结果设定，R<T>，R对象不能序列化，
 * 	所以要让R实现Serializable接口（序列化），redis缓存的注解才能生效。
 */

//结果类，服务端响应的所有结果最终都会包装成此种类型返回给前端页面
//类型是R<T>表示这个类可以在实例化时指定数据的具体类型。表示类中的某些字段或方法可以使用任意类型的数据。
//在实例化时根据需要指定具体的类型，例如 R<String> 或 R<User>。
//R<String> 就是一个 R 类型的对象，其中返回数据的类型是 String。
@Data
public class R<T> implements Serializable {

    private Integer code; //状态编码：1成功，0和其它数字为失败

    private String msg; //错误信息

    //返回的数据，可以是固定的字符串，也可以是对象，根据实际传入的类型而定。这是类级别的泛型参数，用于实例化类时指定类型。
    private T data;

    //创建私有容器，可以存储额外的信息。
    private Map map = new HashMap();

    // 成功时调用此方法，返回信息和编码1
    // 为了能通过类名可以直接调用方法，所以声明静态。
    // 如果静态方法需要使用类的泛型参数data，必须在方法内部自己定义泛型，而不能依赖类的泛型参数。
    // 又因为需要根据参数类型决定数据类型，所以可以使用自定义的泛型方法，在返回值类型前有<T>，
    // 泛型方法声明的 <T> 和类的泛型参数是独立的。
    // 泛型方法的返回值类型是R<T>，根据传入的泛型参数返回一个特定类型的 R 对象。这里的T是泛型方法中的T。
    public static <T> R<T> success(T object) {
        //右侧的<T>可以不写，创建一个带有方法级别泛型 T 的 R 对象
        //比如，员工登录的控制器调用这个方法，参数传递进来的是个employee类型的对象。
        //此时T就是employee，然后我们创建一个泛型是employee类型的r对象。
        //将对象赋值给data,因为data的泛型也是T，现在data也是employee类型，所以直接赋值没有错。
        //最后返回给浏览器的就是一个employee类型的r对象。
        R<T> r = new R<T>();
        //object可能是字符串，也可能是数据对象。因为data属性的泛型是T，所以是根据传入的类型决定的。
        r.data = object;
        r.code = 1; //将1赋值给R对象，表示成功状态。
        return r; //返回封装好的 R 对象。
    }

    // 失败时调用此方法，返回错误信息和编码0
    public static <T> R<T> error(String msg) {
        R r = new R<T>();
        r.msg = msg; //因为错误信息通常就是字符串，所以就是字符串类型
        r.code = 0;
        return r;
    }

    //添加额外的信息
    //方法名为 add，返回值类型 R<T>。它的主要功能是向 map对象中添加一组键值对，并返回当前对象（this），以支持链式调用。
    //如果 key 已存在，则会覆盖原有的值。
    //如果 key 不存在，则添加一个新的键值对。
    public R<T> add(String key, Object value) {
        // 让调用add方法的对象，使用自己的map容器，添加新的数据。
        this.map.put(key, value);
        //支持链式调用，即在调用完 add 方法后，可以继续调用当前对象的其他方法。
        return this;
    }
}
