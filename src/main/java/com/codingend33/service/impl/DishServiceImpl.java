package com.codingend33.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.dto.DishDto;
import com.codingend33.entity.Dish;
import com.codingend33.entity.DishFlavor;
import com.codingend33.mapper.DishMapper;
import com.codingend33.service.DishFlavorService;
import com.codingend33.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {

        //1.将菜品数据保存到dish表中。
        //使用的是MP封装的方法save。因为dishDto继承了dish,所以dish的数据它都有。
        this.save(dishDto);



        //2.将菜品口味数据保存到dish_flavor表，需要装配dishFlavorService的bean
        //菜品口味中的id和口味名称在添加餐品时，是可以封装到菜品口味表中，但是dishId这个属性是NULL。
        //所以需要先获取dishId。dishDto扩展了dishFlavor的参数，所以在控制层调用服务层save（）时，dishId就已经生成了（雪花算法）。
        Long dishId = dishDto.getId();

        //获取Dto中所有的口味数据，是个数组。再通过循环逐一给每个口味中的dishId属性赋值
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor dishFlavor : flavors) {
            dishFlavor.setDishId(dishId);
        }

        // 因为flavors是个集合，所以用saveBatch
        dishFlavorService.saveBatch(flavors);

    }

    @Override
    @Transactional
    public DishDto getByIdWithFlavor(Long id){

        //先根据id查询到对应的dish对象
        Dish dish = this.getById(id);
        //创建一个dishDao对象
        DishDto dishDto = new DishDto();
        //拷贝对象
        BeanUtils.copyProperties(dish,dishDto);
        //条件构造器，对DishFlavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        //根据dish的id与 DishFlavor里的DishId 进行匹配
        queryWrapper.eq(DishFlavor::getDishId, id);
        //查询的结果中封装了DishId对应的菜品口味的数据
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        //并将其赋给dishDto
        dishDto.setFlavors(flavors);
        //作为结果返回给前端，这个dishDto对象中就包含了所有需要回显的数据
        return dishDto;

    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {

        //更新当前菜品数据（dish表中的基础数据）
        this.updateById(dishDto);

        //下面是更新当前菜品的口味数据
        //条件构造器
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        //条件是当前菜品id
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        //将其删除掉（先删除旧的，再添加新口味）
        dishFlavorService.remove(queryWrapper);
        //获取传入的新的口味数据
        List<DishFlavor> flavors = dishDto.getFlavors();
        //这些口味数据还是没有dish_id，所以需要赋予其dishId
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
            //并将item对象封装成一个集合，作为我们的最终结果
        }).collect(Collectors.toList());

        //再重新加入到口味表中
        dishFlavorService.saveBatch(flavors);


    }


}