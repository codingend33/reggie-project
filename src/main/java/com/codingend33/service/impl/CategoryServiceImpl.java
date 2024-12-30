package com.codingend33.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.common.CustomException;
import com.codingend33.entity.Category;
import com.codingend33.entity.Dish;
import com.codingend33.entity.Setmeal;
import com.codingend33.mapper.CategoryMapper;
import com.codingend33.service.CategoryService;
import com.codingend33.service.DishService;
import com.codingend33.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    // 每个菜品和套餐中都关联了一个分类的ID属性。
    // 我们就是通过判断要删除的分类ID是否与餐品和套餐中的分类ID一致，一样就说明是有关联的。

    // 因为要判断是否关联了菜品，所以要使用“菜品”的服务层bean对象
    @Autowired
    private DishService dishService;

    // 因为要判断是否关联了套餐，所以要使用“套餐”的服务层bean对象
    @Autowired
    private SetmealService setmealService;


    // 根据id删除分类，但删除之前需要进行判断，重写接口中的remove方法。
    @Override
    public void remove(Long id) {

        //查看当前分类是否关联了“菜品”，如果已经关联，则抛出异常。

        //创建dish查询对象
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        //添加dish查询条件，在Dish对象中获取分配ID，与前端发送删除分类请求的ID是否一致。
        dishQueryWrapper.eq(Dish::getCategoryId, id);

        //直接调用服务层的count方法，count 方法用于查询数据库中满足特定条件的记录数量。
        //如果数据库中的餐品对象中的分类ID，与 删除分类请求的ID 是相等的，满足条件，计数+1，
        //所以结果只要大于0，就说明这个分类与某些餐品有关联，就不能删除
        long count1 = dishService.count(dishQueryWrapper);
        //方便Debug用的
        log.info("dish查询条件，查询到的条目数为：{}",count1);

        //查看当前分类是否关联了菜品，如果已经关联，则抛出异常
        if (count1 > 0){
            //已关联菜品，抛出一个自定义的异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        //查看当前分类是否关联了“套餐”，如果已经关联，则抛出异常。

        //建setmeal查询对象
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        //添加Setmeal查询条件，在Setmeal对象中获取分配ID，与前端发送删除分类请求的ID是否一致。
        setmealQueryWrapper.eq(Setmeal::getCategoryId,id);

        //直接调用服务层的count方法，count 方法用于查询数据库中满足特定条件的记录数量。
        //如果数据库中的套餐对象中的分类ID，与 删除分类请求的ID 是相等的，满足条件，计数+1，
        //所以结果只要大于0，就说明这个分类与某些套餐有关联，就不能删除
        long count2 = setmealService.count(setmealQueryWrapper);

        //方便Debug用的
        log.info("setmeal查询条件，查询到的条目数为：{}",count2);

        //查看当前分类是否关联了套餐，如果已经关联，则抛出异常
        if (count2 > 0){
            //已关联套餐，抛出一个自定义的异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }

        //走到这里说明没有关联餐品和套餐，就可以使用父类ServiceImpl中的内置删除方法这个分类了。
        super.removeById(id);
    }
}
