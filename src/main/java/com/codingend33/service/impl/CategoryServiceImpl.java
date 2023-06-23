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

    // 每个菜品和套餐中有一个分类的ID属性。
    // 我们就是通过判断要删除的分类ID是否与餐品和套餐中的分类ID一致，一样就说明是有关联的。

    // 因为要判断是否关联了菜品，所以要使用菜品的服务层bean
    @Autowired
    private DishService dishService;

    // 因为要判断是否关联了套餐，所以要使用套餐的服务层bean
    @Autowired
    private SetmealService setmealService;


    // 根据id删除分类，但删除之前需要进行判断
    @Override
    public void remove(Long id) {


        //查看当前分类是否关联了菜品，如果已经关联，则抛出异常。

        //创建dish查询对象
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        //添加dish查询条件，在Dish中获取分配ID，与前端发送删除分类请求的ID是否一致。
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        //直接调用服务层的count方法，统计数量。只要大于0，就说明有关联
        int count1 = dishService.count(dishQueryWrapper);
        log.info("dish查询条件，查询到的条目数为：{}",count1);
        //查看当前分类是否关联了菜品，如果已经关联，则抛出异常
        if (count1 > 0){
            //已关联菜品，抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }


        //查看当前分类是否关联了套餐，如果已经关联，则抛出异常。

        //建setmeal查询对象
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        //添加dish查询条件，根据分类id进行查询
        setmealQueryWrapper.eq(Setmeal::getCategoryId,id);
        int count2 = setmealService.count(setmealQueryWrapper);
        //方便Debug用的
        log.info("setmeal查询条件，查询到的条目数为：{}",count2);
        //查看当前分类是否关联了套餐，如果已经关联，则抛出异常
        if (count2 > 0){
            //已关联套餐，抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }


        //正常删除
        super.removeById(id);
    }
}
