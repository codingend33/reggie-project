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

    //因为保存套餐要操作套餐餐品的数据，所以注入SetmealDishService对象，用于保存套餐餐品部分的数据
    @Autowired
    private SetmealDishService setmealDishService;

    // 重写保存方法
    // 新增套餐，同时要保持套餐与菜品数据
    // 传入的参数就是setmealDto类型
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {

            //1.保存套餐基本信息
            //使用MP封装的save方法即可，因为setmealDto已经继承了setmeal，所以套餐相关的信息会被赋值保存。
            //this调用的是当前类的save方法。
            this.save(setmealDto);

            //2.保存与套餐相关的菜品信息
            //setmealDto扩展了一个 setmealDishes属性，包含了请求中所有餐品相关的数据。将这些数据提取出来
            List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

            //但是每个餐品的setmealID属性是NUll，这个是套餐和餐品餐品的关联数据，非常关键。
            //而setmealDto集成了setmeal，所以它有setmealid这个属性。
            // 那我们就从setmealDto中获取这个参数，然后给setmealDishes中的每个餐品赋值。

            setmealDishes = setmealDishes.stream().map((item) -> {
                item.setSetmealId(setmealDto.getId());
                return item;
                //将所获取的setmealdishes放入一个集合中
            }).collect(Collectors.toList());

            // setmealDishes中的setmealid赋值后，就可以添加到setmealDishe的数据库中了
            // 因为一个套餐有多个餐品，所以使用批量保存。
            setmealDishService.saveBatch(setmealDishes);
    }


    /**
     //重写套餐删除方法
     //因为套餐与餐品是相关联的，所以删除套餐会涉及setmealDish关系表，需要操作两个表。
     //因为起售的套餐不能删除，所以需要先开发起售和停售的功能。
     */

    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {

        //先判断一下能不能删，如果status为1，则套餐在售，不能删，
        //构建一个查询条件。
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //需要满足这个SQL语句：select * from setmeal where id in (ids) and status = 1。这个语句有两个条件：
        //in方法就是指定范围，在数据库中找到在请求参数ids中的id
        setmealLambdaQueryWrapper.in(Setmeal::getId, ids);
        //状态是1，表示起售的。
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        //调用MP的内置方法，得到目前是起售的套餐数量。
        long count = this.count(setmealLambdaQueryWrapper);

        //下面两行是我debug输出的日志，没啥用
        List<Setmeal> list = this.list(setmealLambdaQueryWrapper);
        log.info("查询到的数据为：{}",list);

        //如果count大于0，说明有在售状态的套餐，不能删除。要排除异常。
        if (count > 0) {
            throw new CustomException("套餐正在售卖中，请先停售再进行删除");
        }

        //如果没有在售套餐，则使用MP封装的方法直接批量删除套餐表的数据
        this.removeByIds(ids);

        //删除完套餐信息后，继续删除与套餐关联的餐品数据。setmealDish关联表数据
        //但是不能使用removeByIds方法，因为形参的ids是Setmeal表的主id, 这些id在SetmealDish表中不是主id
        //那我们可以在SetmealDish表中找有这个id的餐品，他们是关联的，再删除他们即可。

        //创建条件查询对象，获取SetmealDish表中有匹配的套餐id的套餐餐品
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //SetmealDish表中的有SetmealId的套餐，与ids匹配
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        //删除有待删除的SetmealId的套餐餐品
        setmealDishService.remove(setmealDishLambdaQueryWrapper);

    }

    /**
     * 通过id获取套餐和餐品的方法
     */

    @Override
    public SetmealDto getByIdWithDish(Long id) {

        //查询套餐基本的信息
        Setmeal setmeal = this.getById(id);

        //新建一个setmealDto对象，
        SetmealDto setmealDto = new SetmealDto();
        //把套餐基本信息赋值到setmealDto对象中，setmealDto继承了setmeal，所以可以接受所有属性
        BeanUtils.copyProperties(setmeal, setmealDto);

        //创建一个查询对象
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        //条件1：SetmealDish中的setmealid 与 Setmeal中的id 进行匹配。
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());

        //将带有setmealid的餐品都封装到一个list中
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        //setmealDto对象的SetmealDishes属性赋值。
        setmealDto.setSetmealDishes(list);

        //此时的setmealDto有setmeal的基础信息，也有所有相应的setmealdish信息。
        return setmealDto;

    }

    /**
       保存更新的套餐和套餐餐品
     */
    @Override
    public void updateWithDish(SetmealDto setmealDto) {

        //先更新setmeal表基本信息
        this.updateById(setmealDto);

        //更新setmeal_dish表信息remove操作。（先删除旧的，再添加信的套餐餐品）
        //先删除原来与套餐关联的菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        //从setmealDto对象中，获取新的套餐餐品信息
        List<SetmealDish> SetmealDishes = setmealDto.getSetmealDishes();

        //新的餐品数据还是没有与Setmeal_id关联，所以需要赋予。
        SetmealDishes = SetmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //将新的套餐餐品保存
        setmealDishService.saveBatch(SetmealDishes);

    }
}