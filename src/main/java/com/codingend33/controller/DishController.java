package com.codingend33.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codingend33.common.CustomException;
import com.codingend33.common.R;
import com.codingend33.dto.DishDto;
import com.codingend33.dto.SetmealDto;
import com.codingend33.entity.Category;
import com.codingend33.entity.Dish;
import com.codingend33.entity.DishFlavor;
import com.codingend33.service.CategoryService;
import com.codingend33.service.DishFlavorService;
import com.codingend33.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;




    //新增餐品
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("接收到的数据为：{}",dishDto);
        dishService.saveWithFlavor(dishDto);
        //清理对应分类的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }


    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        //构造分页构造器对象.使用MP的内置对象Page
        Page<Dish> pageInfo = new Page<>(page,pageSize);

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加name搜索条件
        queryWrapper.like(name!=null,Dish::getName,name);
        //根据更新时间降序排列
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行分页查询
        dishService.page(pageInfo,queryWrapper);


        // 将dish查询出来的分页数据里有分页查询的结果，以及record记录的所有餐品数据，其中category_id是我们需要的。
        // DishDto中有额外的属性category_name。
        // 所以可以将所有dish的数据都复制给DishDto，然后通过category_id去找到category_name

        //创建一个DishDto类型的分页构造器对象
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);

        //将Dish获取到的分页查询的数据pageInfo复制给dishDtoPage（这个复制的是分页查询相关的数据，如当前页，多少条数据等）
        //而records里封装的是在页面展示的所有餐品数据，先拷贝除了records以外的数据，records需要额外处理
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        //获取原records数据，它是一个Dish类型的list集合。
        //record里是餐品数据，其中就有我们需要的category_id，通过这个id去获取category_name。
        List<Dish> records = pageInfo.getRecords();

        //遍历records中每一条数据，赋值给DishDto。
        List<DishDto> list = records.stream().map((item) -> {

            //创建DishDto对象
            DishDto dishDto = new DishDto();

            //将遍历获得的数据复制给dishDto对象。此时只有category_id，还没有categoryName
            BeanUtils.copyProperties(item, dishDto);

            //然后获取一下dish对象的category_id属性（item中有getCategoryId）
            Long categoryId = item.getCategoryId();  //分类id

            //根据这个属性，获取到Category对象（这里需要用@Autowired注入一个CategoryService对象）
            Category category = categoryService.getById(categoryId);

            //随后获取Category对象的name属性，也就是菜品分类名称
            String categoryName = category.getName();

            //最后将菜品分类名称赋给dishDto对象就好了
            dishDto.setCategoryName(categoryName);

            //返回一个dishDto对象，此时对象中就有categoryName属性值了
            return dishDto;

            //并将dishDto对象封装成一个集合，作为我们的最终结果
        }).collect(Collectors.toList());

        //将处理好的list集合赋值给dishDtoPage，此时分页查询结果中就有了所有信息。
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);

    }


    //根据ID查询菜品信息和口味信息
    @GetMapping("/{id}")
    public R<DishDto> getByIdWithFlavor(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        log.info("查询到的数据为：{}", dishDto);
        return R.success(dishDto);
    }

    //菜品修改更新操作
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info("接收到的数据为：{}", dishDto);
        dishService.updateWithFlavor(dishDto);

        //清理对应分类的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }

    //批量停售起售菜品
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable Integer status, @RequestParam List<Long> ids) {
        log.info("status:{},ids:{}", status, ids);
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null, Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, status);

        //清理缓存
        //构造条件查询对象
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //设定条件。匹配数据库中的id与发送请求的ids。
        lambdaQueryWrapper.in(Dish::getId, ids);
        //根据ID获取菜品
        List<Dish> dishes = dishService.list(lambdaQueryWrapper);
        //通过菜品获取分类ID，并动态生成key。
        for (Dish dish : dishes) {
            String key = "dish_" + dish.getCategoryId() + "_1";
            redisTemplate.delete(key);
        }

        dishService.update(updateWrapper);
        return R.success("批量操作成功");
    }


    //批量删除菜品
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("删除的ids：{}", ids);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        queryWrapper.eq(Dish::getStatus, 1);
        int count = dishService.count(queryWrapper);
        if (count > 0) {
            throw new CustomException("删除列表中存在启售状态商品，无法删除");
        }
        dishService.removeByIds(ids);
        return R.success("删除成功");
    }


    //添加套餐功能中，添加菜品的页面，实现这个页面能展示餐品。
    //因为查出的菜品是多个，所以是list集合嵌套着菜品
    //请求连接是dish/list?categoryId=xxx，所以传入的参数使用Dish接收，因为有CategoryId属性

   /* @GetMapping("/list")
    public R<List<Dish>> get(Dish dish){

        //条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //根据传进来的categoryId 与 Dish对象中的CategoryId匹配
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());

        //只查询状态为1的菜品（启售菜品）
        queryWrapper.eq(Dish::getStatus,1);

        //简单排下序，其实也没啥太大作用
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //获取查询到的结果作为返回值
        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/


    //前端的菜品和套餐展示功能开发，对之前的代码进行了修改。
    //方法返回值为DishDto，DishDto继承了Dish，且新增了flavors属性
    @GetMapping("/list")
    public R<List<DishDto>> get(Dish dish){

        //提前声明变量
        List<DishDto> dishDtoList;
        //动态构造Key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //先从redis中获取缓存数据.
        //一个分类下有多个菜品，每个key代表一个分类，也就是一份缓存数据。
        //而且查询的请求是CategoryId和Status参数构成，所以我们动态拼接一下就生成了这key。
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //如果存在，则直接返回，无需查询数据库
         if (dishDtoList != null){
             return R.success(dishDtoList);
         }

        //如果不存在，则查询数据库，继续按照原来的代码执行。
        //条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //根据传进来的categoryId 与 Dish对象中的CategoryId匹配
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());

        //只查询状态为1的菜品（启售菜品）
        queryWrapper.eq(Dish::getStatus,1);

        //简单排下序，其实也没啥太大作用
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //获取查询到的结果作为返回值
        List<Dish> list = dishService.list(queryWrapper);

        //以下是新增的代码(类似上面的page分页查询，因为缺少口味选择，需要使用中间表dishDto)

        //item就是list中的每一条数据，相当于遍历了
        //List<DishDto> dishDtoList = list.stream().map((item) -> {
            dishDtoList = list.stream().map((item) -> {
            //创建一个dishDto对象
            DishDto dishDto = new DishDto();
            //将item的属性全都copy到dishDto里
            BeanUtils.copyProperties(item, dishDto);
            //由于dish表中没有categoryName属性，只存了categoryId
            Long categoryId = item.getCategoryId();
            //所以我们要根据categoryId查询对应的category
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                //然后取出categoryName，赋值给dishDto
                dishDto.setCategoryName(category.getName());
            }
            //然后获取一下菜品id，根据菜品id去dishFlavor表中查询对应的口味，并赋值给dishDto
            Long itemId = item.getId();
            //条件构造器
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            //条件就是菜品id
            lambdaQueryWrapper.eq(itemId != null, DishFlavor::getDishId, itemId);
            //根据菜品id，查询到菜品口味
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);
            //赋给dishDto的对应属性
            dishDto.setFlavors(flavors);
            //并将dishDto作为结果返回
            return dishDto;
            //将所有返回结果收集起来，封装成List
        }).collect(Collectors.toList());

        //最后将查询到的菜品数据添加到缓存中
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);

    }

}
