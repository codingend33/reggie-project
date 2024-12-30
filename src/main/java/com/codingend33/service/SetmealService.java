package com.codingend33.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codingend33.dto.SetmealDto;
import com.codingend33.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    //自定义一个方法用于保存套餐数据（包含菜品数据）
    void saveWithDish(SetmealDto setmealDto);

    //删除套餐
    void removeWithDish(List<Long> ids);

    //修改套餐，有返回值，因为要回显。
    SetmealDto getByIdWithDish(Long id);

    //修改套餐中保存数据
    void updateWithDish(SetmealDto setmealDto);
}
