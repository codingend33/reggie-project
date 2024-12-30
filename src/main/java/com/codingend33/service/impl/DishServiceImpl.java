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

    //因为saveWithFlavor方法也会操控dishFlavor表，所以注入这个bean对象
    @Autowired
    private DishFlavorService dishFlavorService;


    //重写saveWithFlavor方法
    //由于代码涉及多表操作，在启动类上开启事务支持添加@EnableTransactionManagement注解，并且在业务类上添加@Transactional
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {

        //1.将菜品数据保存到dish表。
        //因为dishDto继承了dish,所以dish的属性它都有。使用的是MP封装的方法save方法即可。将dishDto中的dish相关的属性保存到dish表中。
        //this：表示调用当前类（DishServiceImpl）中定义的或继承的方法。如果当前类重写了父类的方法，this 调用的是重写后的方法。
        //因为目前没有重写save方法，所以和super.save没有区别，调用的就是父类ServiceImpl中的save方法。
        this.save(dishDto);

        //2.将菜品口味数据保存到dish_flavor表，需要装配dishFlavorService的bean以操作dish_flavor表
        // dishDto类中封装了一个flavors数组，每个元素存放的是请求参数中餐品口味的name和value属性。
        // 这个数组的类型是dishFlavor，也就是说数组中的每一组口味都存在一个dishFlavor的对象中，每个对象的name和value属性有赋值。
        // 但是dishFlavor对象中的dishId这个属性是NULL，没有被赋值，这个参数是很关键的，它对应的是餐品id,才能将口味和菜品关联起来。

        //所以在保存口味数据前，需要先获取dishId。
        //因为dishDto继承了dish的所有属性，所以它肯定保存了dishid，所以可以取出dishDto的dishId，然后对flavor数组中的每一个口味对象中的dishId赋值.

        //DishDto 中的dishid 属性会被自动填充（雪花算法生成的主键）。
        //我们最开始调用 this.save(dishDto)时，将 dishDto 对象中的dish属性保存到 dish 表时，就生成了dishId。
        //使用dishDto.getId() 获取dishid
        Long dishId = dishDto.getId();

        //DishDto中flavors是个数组，数组中存的是DishFlavor口味对象，每个对象是一组口味
        List<DishFlavor> flavors = dishDto.getFlavors();

        //获取到以后再通过循环逐一给每组口味中的dishId属性赋值
        for (DishFlavor dishFlavor : flavors) {
            dishFlavor.setDishId(dishId);
        }

        // 完成dishid的赋值后再批量保存。在dishDto中，flavors是个集合，所以用saveBatch去保存。
        dishFlavorService.saveBatch(flavors);

    }


    //重写根据餐品ID获取dishflavor和dish
    //由于代码涉及多表操作，在启动类上开启事务支持添加@EnableTransactionManagement注解，并且在业务类上添加@Transactional
    @Override
    @Transactional
    public DishDto getByIdWithFlavor(Long id){

        //先根据id查询到对应的dish对象
        Dish dish = this.getById(id);
        //创建一个dishDao对象
        DishDto dishDto = new DishDto();

        //将dish对象现有的属性拷贝到dishDto对象
        BeanUtils.copyProperties(dish,dishDto);

        //条件构造器，因为是要查询口味，所以对DishFlavor表查询，那构造器类型是DishFlavor
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        // 每个dish都有自己的口味，而dishid是与这些口味的关联点。
        // DishFlavor里的DishId 与 请求参数中dish的id 进行匹配
        queryWrapper.eq(DishFlavor::getDishId, id);

        // 调用DishFlavor的服务层，获取DishId对应菜品的口味数据
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);

        //将口味信息赋给dishDto中的Flavors属性。
        dishDto.setFlavors(flavors);

        //作为结果返回给前端，这个dishDto对象中就包含了所有需要回显的数据
        return dishDto;
    }


    /**
     * 更新菜品保存
     * 传入的是一个DishDto类型的对象。这个对象中有dish基础信息和扩展的口味信息数组
     * 但是每组口味信息中的dishid是空的，可以通过dishDto对象获取dishid，然后给每组口味信息赋值id再保存
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {

        //1.将dishDto对象中，dish的基础数据，保存到dish表中
        //因为dishDto继承了dish,所以dish的属性它都有。使用的是MP封装的方法updateById方法即可。将dishDto中的dish相关的属性保存到dish表中。
        //this：表示调用当前类（DishServiceImpl）中定义的或继承的方法。如果当前类重写了父类的方法，this 调用的是重写后的方法。
        //因为目前没有重写save方法，所以和super.save没有区别，调用的就是父类ServiceImpl中的save方法。
        this.updateById(dishDto);

        //2.更新当前菜品关联的口味数据

        //2.1需要先删除原本的口味数据
        //条件构造器，因为操作的是DishFlavor，所以类型是DishFlavor
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        //条件。匹配餐品口味表中的dishid 与 提交的参数中的dishid一致的口味数据
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        //先删除旧的口味数据
        dishFlavorService.remove(queryWrapper);

        //2.2 添加新的口味数据
        // 传进来的dishDto对象中有所有新的口味数据
        List<DishFlavor> flavors = dishDto.getFlavors();

        //但是dishDto对象封装的时候只赋值了name和value属性，并没有dish_id，所以需要先赋予其dishId
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
            //并将item对象封装成一个集合，作为我们的最终结果
        }).collect(Collectors.toList());

        //再重新将新的口味批量保存。
        dishFlavorService.saveBatch(flavors);
    }
}