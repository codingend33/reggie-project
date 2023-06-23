package com.codingend33.dto;

import com.codingend33.entity.Setmeal;
import com.codingend33.entity.SetmealDish;
import lombok.Data;

import java.util.List;


@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
