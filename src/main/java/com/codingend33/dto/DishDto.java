package com.codingend33.dto;

import com.codingend33.entity.Dish;
import com.codingend33.entity.DishFlavor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DishDto extends Dish {

    //DishDto继承了dish,所以它有dish的所有属性。自己又扩展了flavors属性，所以这个类就与前端发送的数据参数能一一对应上。
    private List<DishFlavor> flavors = new ArrayList<>();

    //后面这两条属性暂时没用，这里只需要用第一条属性
    private String categoryName;

    private Integer copies;
}