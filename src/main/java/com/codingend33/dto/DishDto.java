package com.codingend33.dto;

import com.codingend33.entity.Dish;
import com.codingend33.entity.DishFlavor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


/**
 * DishDto用于满足请求参数与实体属性不是一一对应的情况下进行数据传输，
 * DishDto继承dish以得到dish全部的属性，然后再扩展一些新的属性。
 *
 *  @Data 注解
 * 来源：lombok.Data。
 * 作用：自动生成以下内容，简化代码：
 * Getter 和 Setter：为所有属性生成对应的 get 和 set 方法。
 * toString() 方法：自动生成 toString() 方法，用于打印类内容。
 * equals() 和 hashCode() 方法：用于对象比较和集合操作。
 * 无参构造方法。
 */
@Data
public class DishDto extends Dish {

    //DishDto继承了dish,所以它有dish的所有属性。自己又扩展了flavors属性，以满足与前端发送的数据参数能一一对应上。
    //这个flavors属性名字 与请求参数中flavors是一致的，才能接受这些参数。

    //请求参数中的flavors属性的结构是一个数组，里面存的是name和value属性，而DishFlavor实体类就包含这些属性，的所以泛型使用DishFlavor。
    //也就是将flavors中每一组口味都封装为一个DishFlavor类的对象，每个对象中name和value属性接受的请求中的一组参数。
    //这里就存在一个问题，就是DishFlavor对象的dishID没有赋值。这个在dishservice实现类中解决
    private List<DishFlavor> flavors = new ArrayList<>();

    //在菜品分页查询时，需要这个属性。因为分页结果中需要展示餐品的分类名称，但dish对象中只有id。
    //所以给dishdto扩展这个属性，然后分页查询时返回dishdto对象就能正确展示。
    private String categoryName;

    private Integer copies;
}