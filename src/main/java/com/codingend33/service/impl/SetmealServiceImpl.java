package com.codingend33.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.common.CustomException;
import com.codingend33.dto.SetmealDto;
import com.codingend33.entity.Setmeal;
import com.codingend33.entity.SetmealDish;
import com.codingend33.mapper.SetmealMapper;
import com.codingend33.service.SetmealDishService;
import com.codingend33.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;


    //新增套餐，同时要保持套餐与菜品的关联关系
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {

            //保存套餐基本信息，操作setmeal，执行insert操作。
            //使用MP封装的save方法即可，而且setmealDto已经继承了setmeal，所以将它传进去就可以。
            this.save(setmealDto);


            //保存套餐和菜品的关联信息，操作setmeal_dish，执行insert操作

            //通过setmealDto就能获取到SetmealDishes的所有属性，
            List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

            //但是这里面的setmealID属性是NUll，所以先对这个集合进行处理，通过遍历获取这个属性，然后给他赋值ID
            setmealDishes = setmealDishes.stream().map((item) -> {
                item.setSetmealId(setmealDto.getId());
                return item;
                //将所获取的setmealdishes放入一个集合中
            }).collect(Collectors.toList());

            //因为要操做setmeal_dish，所以就要装配SetmealDishService bean对象
            setmealDishService.saveBatch(setmealDishes);
    }

    //重写套餐删除方法

    //因为套餐与餐品是相关联的，所以删除套餐会涉及setmealDish关系表，需要操作两个表。
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //先判断一下能不能删，如果status为1，则套餐在售，不能删，
        //构建一个查询条件。
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //需要满足这个SQL语句：select * from setmeal where id in (ids) and status = 1。这个语句有两个条件：
        setmealLambdaQueryWrapper.in(Setmeal::getId, ids);
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        //调用MP的内置方法，得到满足条件的ID数量
        int count = this.count(setmealLambdaQueryWrapper);

        //下面两行是我debug输出的日志，没啥用
        List<Setmeal> list = this.list(setmealLambdaQueryWrapper);
        log.info("查询到的数据为：{}",list);

        //如果count大于0，说明有在售状态的套餐，不能删除。要排除异常。
        if (count > 0) {
            throw new CustomException("套餐正在售卖中，请先停售再进行删除");
        }

        //如果没有在售套餐，则使用MP封装的方法直接删除套餐表的数据
        this.removeByIds(ids);

        //继续删除setmealDish关联表数据
        //这里不能使用removeByIds方法，因为它使用的是setmeal表中的D,这里需要的是setmealDish中的ID
        //创建条件查询对象
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);

        setmealDishService.remove(setmealDishLambdaQueryWrapper);

    }

    @Override
    public SetmealDto getByIdWithDish(Long id) {
        //查询套餐基本信息
        Setmeal setmeal = this.getById(id);

        //查询套餐菜品信息
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        setmealDto.setSetmealDishes(list);
        return setmealDto;

    }

    @Override
    public void updateWithDish(SetmealDto setmealDto) {

        //更新setmeal表基本信息
        this.updateById(setmealDto);

        //更新setmeal_dish表信息remove操作。（先删除旧的，再添加信的套餐餐品）
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        //更新setmeal_dish表信息insert操作
        List<SetmealDish> SetmealDishes = setmealDto.getSetmealDishes();

        //这些口味数据还是没有Setmeal_id，所以需要赋予。
        SetmealDishes = SetmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //将新的套餐餐品保存
        setmealDishService.saveBatch(SetmealDishes);

    }
}