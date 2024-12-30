package com.codingend33.dto;

import com.codingend33.entity.Setmeal;
import com.codingend33.entity.SetmealDish;
import lombok.Data;

import java.util.List;


/**
 * 继承了setmeal,所以SetmealDto有setmeal的所有属性
 *
 */
@Data
public class SetmealDto extends Setmeal {

    //扩展一个套餐中所有餐品的数组
    private List<SetmealDish> setmealDishes;

    //扩展一个套餐分类名称
    private String categoryName;
}
