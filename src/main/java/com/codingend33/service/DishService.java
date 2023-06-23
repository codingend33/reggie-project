package com.codingend33.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codingend33.dto.DishDto;
import com.codingend33.entity.Dish;

public interface DishService extends IService<Dish> {

    //新增菜品同时插入菜品对应的口味数据,需要操作两张表：dish、dishflavor
    void saveWithFlavor(DishDto dishDto);

    //修改餐品时，根据餐品ID获取dishflavor
    DishDto getByIdWithFlavor(Long id);


    //修改菜品的更新保存
    void updateWithFlavor(DishDto dishDto);

}
