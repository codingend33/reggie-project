package com.codingend33.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codingend33.dto.DishDto;
import com.codingend33.entity.Dish;

public interface DishService extends IService<Dish> {

    //新增菜品要同时插入菜品和口味数据,需要操作两张表：dish、dishflavor
    //所以需要自定义一个方法，参数就是我们新定义的dto对象，封装了所有的请求参数。
    void saveWithFlavor(DishDto dishDto);


    //修改餐品时，根据餐品ID获取dishflavor和dish.
    //有返回值类型是DishDto
    DishDto getByIdWithFlavor(Long id);


    //修改菜品的更新保存，没有返回值。参数类型是DishDto
    void updateWithFlavor(DishDto dishDto);

}
